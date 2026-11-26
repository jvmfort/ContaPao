package contapao.persistencia;

import contapao.expedicao.StatusCiclo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class RepositorioReposicaoSQLite implements RepositorioReposicao {

    private final String urlConexao;

    public RepositorioReposicaoSQLite(String caminhoArquivoDb) {
        this.urlConexao = "jdbc:sqlite:" + caminhoArquivoDb;
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(urlConexao);
    }

    @Override
    public void inicializarBanco() {
        String sqlEstoque = "CREATE TABLE IF NOT EXISTS estoque_central (insumo TEXT PRIMARY KEY, saldo INTEGER NOT NULL CHECK(saldo >= 0));";
        String sqlCiclo = "CREATE TABLE IF NOT EXISTS ciclo (id INTEGER PRIMARY KEY, estado TEXT NOT NULL);";
        String sqlDist = "CREATE TABLE IF NOT EXISTS distribuicao (ciclo_id INTEGER, insumo TEXT, unidade TEXT, qtd_atendida INTEGER, PRIMARY KEY(ciclo_id, insumo, unidade));";

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlEstoque);
            stmt.execute(sqlCiclo);
            stmt.execute(sqlDist);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco SQLite: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetarDadosDemonstracao() {
        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM distribuicao;");
            stmt.execute("DELETE FROM ciclo;");
            stmt.execute("DELETE FROM estoque_central;");
            stmt.execute("INSERT INTO estoque_central (insumo, saldo) VALUES ('Farinha de Trigo 50kg', 100);");
            stmt.execute("INSERT INTO ciclo (id, estado) VALUES (1, 'ENCERRADO');");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao resetar dados iniciais: " + e.getMessage(), e);
        }
    }

    @Override
    public int consultarSaldoCentral(String insumo) {
        String sql = "SELECT saldo FROM estoque_central WHERE insumo = ?;";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, insumo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("saldo");
            }
            throw new IllegalArgumentException("Insumo não encontrado: " + insumo);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar saldo central: " + e.getMessage(), e);
        }
    }

    @Override
    public void cadastrarEstoque(String insumo, int quantidade) {
        if (insumo == null || insumo.isBlank()) {
            throw new IllegalArgumentException("Insumo não informado.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade a cadastrar deve ser maior que zero.");
        }
        String sql = "INSERT INTO estoque_central (insumo, saldo) VALUES (?, ?) "
                + "ON CONFLICT(insumo) DO UPDATE SET saldo = saldo + excluded.saldo;";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, insumo);
            ps.setInt(2, quantidade);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar estoque: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean baixarSaldoCentral(String insumo, int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade a baixar não pode ser negativa.");
        }
        String sql = "UPDATE estoque_central SET saldo = saldo - ? WHERE insumo = ? AND saldo >= ?;";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setString(2, insumo);
            ps.setInt(3, quantidade);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao baixar saldo central: " + e.getMessage(), e);
        }
    }

    @Override
    public void criarCiclo(int cicloId) {
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ciclo (id, estado) VALUES (?, 'ABERTO');")) {
            ps.setInt(1, cicloId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            if (msg.contains("constraint") || msg.contains("unique")) {
                throw new IllegalArgumentException("Ciclo já existe: " + cicloId, e);
            }
            throw new RuntimeException("Erro ao criar ciclo: " + e.getMessage(), e);
        }
    }

    @Override
    public void encerrarCiclo(int cicloId) {
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE ciclo SET estado = 'ENCERRADO' WHERE id = ? AND estado = 'ABERTO';")) {
            ps.setInt(1, cicloId);
            if (ps.executeUpdate() == 0) {
                StatusCiclo atual = consultarEstadoCiclo(cicloId);
                throw new IllegalStateException("Só é possível encerrar um ciclo ABERTO (estado atual: " + atual + ").");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao encerrar ciclo: " + e.getMessage(), e);
        }
    }

    @Override
    public StatusCiclo consultarEstadoCiclo(int cicloId) {
        String sql = "SELECT estado FROM ciclo WHERE id = ?;";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cicloId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return StatusCiclo.valueOf(rs.getString("estado"));
            }
            throw new IllegalArgumentException("Ciclo não encontrado: " + cicloId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar estado do ciclo: " + e.getMessage(), e);
        }
    }

    @Override
    public void registrarDistribuicaoAtomica(int cicloId, String insumo, int totalDistribuido,
                                             Map<String, Integer> atendimentosPorUnidade) {
        if (totalDistribuido < 0) {
            throw new IllegalArgumentException("Total distribuído não pode ser negativo.");
        }

        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE ciclo SET estado = 'DISTRIBUIDO' WHERE id = ? AND estado = 'ENCERRADO';")) {
                    ps.setInt(1, cicloId);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("Ciclo não está ENCERRADO ou já foi distribuído.");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE estoque_central SET saldo = saldo - ? WHERE insumo = ? AND saldo >= ?;")) {
                    ps.setInt(1, totalDistribuido);
                    ps.setString(2, insumo);
                    ps.setInt(3, totalDistribuido);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("Insumo inexistente ou saldo central insuficiente.");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO distribuicao (ciclo_id, insumo, unidade, qtd_atendida) VALUES (?, ?, ?, ?);")) {
                    for (Map.Entry<String, Integer> e : atendimentosPorUnidade.entrySet()) {
                        ps.setInt(1, cicloId);
                        ps.setString(2, insumo);
                        ps.setString(3, e.getKey());
                        ps.setInt(4, e.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro na distribuição: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> consultarAtendimentos(int cicloId, String insumo) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT unidade, qtd_atendida FROM distribuicao WHERE ciclo_id = ? AND insumo = ? ORDER BY rowid;";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cicloId);
            ps.setString(2, insumo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString("unidade"), rs.getInt("qtd_atendida"));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar atendimentos: " + e.getMessage(), e);
        }
    }
}
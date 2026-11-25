package persistencia;

import expedicao.StatusCiclo;

import java.sql.*;
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
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, insumo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("saldo");
            throw new IllegalArgumentException("Insumo não encontrado: " + insumo);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar saldo central: " + e.getMessage(), e);
        }
    }

    @Override
    public StatusCiclo consultarEstadoCiclo(int cicloId) {
        String sql = "SELECT estado FROM ciclo WHERE id = ?;";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cicloId);
            try (ResultSet rs = pstmt.executeQuery()) {
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
            conn.setAutoCommit(false); // transação tudo-ou-nada (RN06)
            try {
                // 1. Só passa se o ciclo ainda estiver ENCERRADO (RN05, sem corrida)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE ciclo SET estado = 'DISTRIBUIDO' WHERE id = ? AND estado = 'ENCERRADO';")) {
                    ps.setInt(1, cicloId);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("Ciclo não está ENCERRADO ou já foi distribuído.");
                    }
                }

                // 2. Baixa atômica do saldo: falha se o saldo não cobrir
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE estoque_central SET saldo = saldo - ? WHERE insumo = ? AND saldo >= ?;")) {
                    ps.setInt(1, totalDistribuido);
                    ps.setString(2, insumo);
                    ps.setInt(3, totalDistribuido);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException("Insumo inexistente ou saldo central insuficiente.");
                    }
                }

                // 3. Grava o que cada unidade recebeu
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
                throw ex; // preserva o tipo original do erro
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro na distribuição (rollback executado): " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> consultarAtendimentos(int cicloId, String insumo) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT unidade, qtd_atendida FROM distribuicao WHERE ciclo_id = ? AND insumo = ?;";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cicloId);
            pstmt.setString(2, insumo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                mapa.put(rs.getString("unidade"), rs.getInt("qtd_atendida"));
            }
            return mapa;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar atendimentos: " + e.getMessage(), e);
        }
    }
}
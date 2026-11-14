package com.example.servicehub_api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("com.example.servicehub_api.PostgresTestDatabase#isEnabled")
// Flyway creates isolated schemas at runtime, including the legacy V8 tables.
@SuppressWarnings("SqlResolve")
class ReferenceSchemaPostgresTest {
    private final String url = PostgresTestDatabase.url();
    private final String user = PostgresTestDatabase.username();
    private final String password = PostgresTestDatabase.password();
    private Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema).target(target).load();
    }
    private String schema(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", ""); }
    private Connection connect(String schema) throws SQLException {
        Connection connection = DriverManager.getConnection(url,user,password);
        connection.setSchema(schema);
        return connection;
    }
    @Test void migracoesGeramAsMesmasColunasConstraintsIndicesEComentariosDoProfessor() throws Exception {
        String actual = schema("actual_");
        String expected = schema("reference_");
        flyway(actual,"latest").migrate();
        try (Connection c = connect("public"); Statement statement = c.createStatement()) { statement.execute("CREATE SCHEMA " + expected); }
        try (Connection c = connect(expected); Statement statement = c.createStatement()) {
            String reference = new String(Objects.requireNonNull(getClass().getResourceAsStream("/reference/servicehub_schema.sql")).readAllBytes(), StandardCharsets.UTF_8);
            statement.execute(reference);
        }
        String tables = " AND t.relname IN ('users','services','service_requests','reviews') ";
        String common = " FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace ";
        String columns = "SELECT t.relname,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull,COALESCE(pg_get_expr(d.adbin,d.adrelid),''),COALESCE(col_description(t.oid,a.attnum),'')"
                + common + " JOIN pg_attribute a ON a.attrelid=t.oid LEFT JOIN pg_attrdef d ON d.adrelid=t.oid AND d.adnum=a.attnum WHERE n.nspname=? AND a.attnum>0 AND NOT a.attisdropped" + tables + " ORDER BY t.relname,a.attnum";
        String constraints = "SELECT t.relname,x.conname,x.contype,pg_get_constraintdef(x.oid)" + common
                + " JOIN pg_constraint x ON x.conrelid=t.oid WHERE n.nspname=?" + tables + " ORDER BY t.relname,x.conname";
        String indexes = "SELECT t.relname,i.relname,pg_get_indexdef(i.oid)" + common
                + " JOIN pg_index x ON x.indrelid=t.oid JOIN pg_class i ON i.oid=x.indexrelid WHERE n.nspname=?" + tables + " ORDER BY t.relname,i.relname";
        assertEquals(metadata(expected, columns),metadata(actual, columns),"Colunas, defaults e comentários");
        assertEquals(metadata(expected, constraints),metadata(actual, constraints),"Restrições e chaves estrangeiras");
        assertEquals(metadata(expected, indexes),metadata(actual, indexes),"Índices");
        String comments = "SELECT t.relname,COALESCE(obj_description(t.oid),'')" + common
                + " WHERE n.nspname=?" + tables + " ORDER BY t.relname";
        assertEquals(metadata(expected, comments),metadata(actual, comments),"Comentários das tabelas");
    }
    private List<String> metadata(String schema, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection c = connect(schema); PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1,schema);
            try (ResultSet result = statement.executeQuery()) {
                while(result.next()) {
                    List<String> values = new ArrayList<>();
                    for (int i=1;i<=result.getMetaData().getColumnCount();i++) values.add(String.valueOf(result.getObject(i)).replace(schema + ".", ""));
                    rows.add(String.join("|",values));
                }
            }
        }
        return rows;
    }
    private void insertLegacy(String schema) throws SQLException {
        try (Connection c = connect(schema); Statement s = c.createStatement()) {
            s.execute("INSERT INTO clientes(id,nome,email,telefone,documento) VALUES (17,'Cliente legado','CLIENTE@example.com','11999999999','52998224725')");
            s.execute("INSERT INTO prestadores(id,nome,cpf,especialidade,telefone) VALUES (3,'Prestador legado','52998224725','Limpeza','11999999999')");
            s.execute("INSERT INTO servicos(id,titulo,descricao,preco,prestador_id) VALUES (9,'Limpeza','Apartamento',125.00,3)");
            s.execute("INSERT INTO contratos(id,descricao_servico,valor_total,status,cliente_id,prestador_id,servico_id) VALUES (41,'Contrato catálogo',100.00,'CONCLUIDO',17,3,9),(42,'Contrato avulso',50.00,'AGENDADO',17,3,NULL)");
            s.execute("INSERT INTO avaliacoes(id,nota,comentario,contrato_id) VALUES (7,5,'Muito bom',41)");
            s.execute("INSERT INTO pagamentos(id,valor_pago,metodo_pagamento,contrato_id) VALUES (6,100.00,'PIX',41)");
        }
    }
    @Test void preservaDadosLegadosVinculosPrecosEDocumentosSemCriarSenhasDeDemonstracao() throws Exception {
        String schema = schema("legacy_");
        flyway(schema,"8").migrate(); insertLegacy(schema); flyway(schema,"latest").migrate();
        try (Connection c = connect(schema); Statement s = c.createStatement()) {
            assertEquals("cliente@example.com", scalar(s,"SELECT email FROM users WHERE id=17"));
            assertEquals("2",scalar(s,"SELECT count(*) FROM users WHERE NOT is_active AND length(password_hash)=60"));
            assertEquals("20",scalar(s,"SELECT provider_id FROM services WHERE id=9"));
            assertEquals("100.00",scalar(s,"SELECT total_price FROM service_requests WHERE id=41"));
            assertEquals("COMPLETED",scalar(s,"SELECT status FROM service_requests WHERE id=41"));
            assertEquals("PENDING",scalar(s,"SELECT status FROM service_requests WHERE id=42"));
            try (ResultSet inactive = s.executeQuery("SELECT is_active FROM services WHERE id=51")) {
                assertTrue(inactive.next());
                assertFalse(inactive.getBoolean(1));
            }
            assertEquals("17",scalar(s,"SELECT reviewer_id FROM reviews WHERE id=7"));
            assertEquals("52998224725",scalar(s,"SELECT cpf FROM legacy_prestadores WHERE id=3"));
            assertEquals("41",scalar(s,"SELECT contrato_id FROM pagamentos WHERE id=6"));
            assertEquals("21",scalar(s,"SELECT nextval(pg_get_serial_sequence('users','id'))"));
            assertThrows(SQLException.class, () -> s.execute("INSERT INTO reviews(request_id,reviewer_id,rating) VALUES (41,17,4)"));
        }
    }
    @Test void duplicidadeDeEmailInterrompeMigracaoSemPerderDadosEPermiteRetomar() throws Exception {
        String schema = schema("conflict_");
        flyway(schema,"8").migrate(); insertLegacy(schema);
        try (Connection c=connect(schema);Statement s=c.createStatement()) {
            s.execute("INSERT INTO clientes(id,nome,email) VALUES (18,'Duplicado','cliente@EXAMPLE.com')");
        }
        assertThrows(RuntimeException.class, () -> flyway(schema,"latest").migrate());
        try (Connection c=connect(schema);Statement s=c.createStatement()) {
            assertEquals("2",scalar(s,"SELECT count(*) FROM clientes"));
            assertEquals("0",scalar(s,"SELECT count(*) FROM users"));
            s.execute("UPDATE clientes SET email='outro@example.com' WHERE id=18");
        }
        flyway(schema,"latest").migrate();
        try (Connection c=connect(schema);Statement s=c.createStatement()) {
            assertEquals("3",scalar(s,"SELECT count(*) FROM users"));
        }
    }
    private String scalar(Statement s,String sql) throws SQLException {
        try(ResultSet result=s.executeQuery(sql)) { assertTrue(result.next()); return result.getString(1); }
    }
}

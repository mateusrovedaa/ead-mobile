package br.univates.ead.treinodb

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Toda a conversa com o SQLite acontece aqui dentro.
 *
 * `SQLiteOpenHelper` e a classe do proprio Android que cuida do ciclo de vida
 * do arquivo do banco: ela verifica se o arquivo existe, cria se nao existir,
 * e chama [onUpgrade] quando a versao declarada muda. E obrigatorio sobrescrever
 * [onCreate] e [onUpgrade].
 *
 * Por que juntar tudo numa classe so? Porque a Activity nao deveria saber
 * escrever SQL. Ela pede "me devolve a lista de treinos" e recebe uma
 * `List<Treino>`. Se amanha o banco virar Room, ou virar um servidor na
 * internet, so este arquivo muda.
 */
class TreinoDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    NOME_BANCO,
    null, // CursorFactory: quase ninguem usa, passa-se null.
    VERSAO_BANCO,
) {

    /**
     * Roda UMA unica vez: na primeira vez que o app pede o banco e o arquivo
     * ainda nao existe. Se voce ja rodou o app e depois mexer neste SQL, este
     * metodo NAO roda de novo — quem roda e o [onUpgrade].
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABELA (
                $COLUNA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUNA_ATIVIDADE TEXT NOT NULL,
                $COLUNA_TIPO TEXT NOT NULL,
                $COLUNA_DATA TEXT NOT NULL,
                $COLUNA_AQUECIMENTO INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
    }

    /**
     * Roda quando o [VERSAO_BANCO] declarado no codigo e MAIOR que a versao
     * gravada no arquivo do aparelho — ou seja, quando o usuario atualiza o app.
     *
     * A estrategia abaixo (derrubar e recriar) e a mais simples e e a que apaga
     * todos os dados do usuario. Serve para um app de aula. Em um app de
     * verdade a migracao preserva o que ja existe, mais ou menos assim:
     *
     *     if (oldVersion < 2) {
     *         db.execSQL("ALTER TABLE treino ADD COLUMN duracao INTEGER DEFAULT 0")
     *     }
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABELA")
        onCreate(db)
    }

    /**
     * Grava um treino e devolve o id gerado (ou -1 se falhou).
     *
     * [ContentValues] e um mapa de "nome da coluna" para valor. E o jeito do
     * Android de montar o INSERT sem escrever SQL na mao — e, de quebra, ele
     * escapa os valores, o que elimina o risco de SQL injection.
     */
    fun inserir(atividade: String, tipo: String, data: String, aquecimento: Boolean): Long {
        val valores = ContentValues().apply {
            put(COLUNA_ATIVIDADE, atividade)
            put(COLUNA_TIPO, tipo)
            put(COLUNA_DATA, data)
            // SQLite nao tem tipo booleano: guarda-se 0 ou 1 num INTEGER.
            put(COLUNA_AQUECIMENTO, if (aquecimento) 1 else 0)
        }
        return writableDatabase.insert(TABELA, null, valores)
    }

    /**
     * Le a tabela inteira e devolve uma lista pronta para a tela.
     *
     * O [Cursor] e um ponteiro que caminha pelas linhas do resultado — ele NAO
     * e a lista. Comeca antes da primeira linha, e cada `moveToNext()` avanca
     * uma posicao e devolve `false` quando acaba.
     *
     * O `use { }` fecha o Cursor no final, mesmo se der excecao no meio. Cursor
     * que nao e fechado vaza memoria e o Android reclama no Logcat.
     */
    fun listar(ordem: Ordem): List<Treino> {
        val treinos = mutableListOf<Treino>()

        // `ordem.clausula` vem de um enum, entao nao ha texto do usuario indo
        // parar dentro do SQL. Concatenar algo digitado pelo usuario aqui seria
        // exatamente a brecha de SQL injection.
        val cursor = readableDatabase.query(
            TABELA, // tabela
            null, // colunas: null = todas
            null, // WHERE
            null, // argumentos do WHERE
            null, // GROUP BY
            null, // HAVING
            ordem.clausula, // ORDER BY
        )

        cursor.use {
            while (it.moveToNext()) {
                treinos.add(
                    Treino(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUNA_ID)),
                        atividade = it.getString(it.getColumnIndexOrThrow(COLUNA_ATIVIDADE)),
                        tipo = it.getString(it.getColumnIndexOrThrow(COLUNA_TIPO)),
                        data = it.getString(it.getColumnIndexOrThrow(COLUNA_DATA)),
                        // De volta de 0/1 para true/false.
                        aquecimento = it.getInt(it.getColumnIndexOrThrow(COLUNA_AQUECIMENTO)) == 1,
                    ),
                )
            }
        }
        return treinos
    }

    /** Quantas linhas existem na tabela. */
    fun contar(): Long = android.database.DatabaseUtils.queryNumEntries(readableDatabase, TABELA)

    /**
     * Altera um registro existente. Os tres argumentos que importam sao a
     * tabela, os valores novos e o WHERE.
     *
     * O `?` no WHERE e um marcador de posicao: o valor real vai no array
     * seguinte. Nunca monte o WHERE concatenando string.
     *
     * Devolve quantas linhas foram alteradas.
     */
    fun atualizar(id: Long, atividade: String, tipo: String, data: String, aquecimento: Boolean): Int {
        val valores = ContentValues().apply {
            put(COLUNA_ATIVIDADE, atividade)
            put(COLUNA_TIPO, tipo)
            put(COLUNA_DATA, data)
            put(COLUNA_AQUECIMENTO, if (aquecimento) 1 else 0)
        }
        return writableDatabase.update(
            TABELA,
            valores,
            "$COLUNA_ID = ?",
            arrayOf(id.toString()),
        )
    }

    /**
     * Apaga um registro. Devolve quantas linhas sairam.
     *
     * Cuidado classico: passar `null` no WHERE apaga a tabela inteira.
     */
    fun excluir(id: Long): Int = writableDatabase.delete(
        TABELA,
        "$COLUNA_ID = ?",
        arrayOf(id.toString()),
    )

    /** Esvazia a tabela. Aqui o WHERE nulo e proposital. */
    fun excluirTodos(): Int = writableDatabase.delete(TABELA, null, null)

    /** As ordenacoes possiveis da listagem, usadas pelo menu. */
    enum class Ordem(val clausula: String) {
        MAIS_RECENTE("$COLUNA_ID DESC"),
        ATIVIDADE("$COLUNA_ATIVIDADE COLLATE NOCASE ASC"),
    }

    companion object {
        /** Nome do arquivo criado em /data/data/<pacote>/databases/. */
        const val NOME_BANCO = "treinos.db"

        /**
         * Suba este numero sempre que mexer no CREATE TABLE. E ele que dispara
         * o [onUpgrade] no aparelho de quem ja tinha o app instalado.
         */
        const val VERSAO_BANCO = 1

        const val TABELA = "treino"
        const val COLUNA_ID = "id"
        const val COLUNA_ATIVIDADE = "atividade"
        const val COLUNA_TIPO = "tipo"
        const val COLUNA_DATA = "data"
        const val COLUNA_AQUECIMENTO = "aquecimento"
    }
}

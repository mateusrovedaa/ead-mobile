package br.univates.ead.treinodb

/**
 * Um registro da tabela `treino`.
 *
 * `data class` e um atalho do Kotlin: o compilador escreve sozinho o
 * construtor, os getters, o `equals`, o `hashCode`, o `toString` e o `copy`.
 * Em Java isso seria uma classe com cinco campos e umas cinquenta linhas de
 * codigo repetitivo.
 *
 * Repare que esta classe nao sabe nada sobre banco de dados. Ela e so o
 * formato do dado; quem conversa com o SQLite e o [TreinoDbHelper].
 */
data class Treino(
    val id: Long,
    val atividade: String,
    val tipo: String,
    val data: String,
    val aquecimento: Boolean,
)

package br.univates.ead.treinodb

import android.content.Context

/**
 * Guarda escolhas do usuario que nao sao "dados", e sim preferencia de uso.
 *
 * Regra pratica para decidir onde guardar cada coisa:
 *
 *   SharedPreferences -> poucos valores soltos (o tema escolhido, o ultimo
 *                        filtro usado, se ja mostrou o tutorial).
 *   SQLite / Room     -> muitos registros com estrutura, que se consulta,
 *                        ordena e filtra. E o caso da lista de treinos.
 *
 * Aqui guardamos duas coisas: qual ordenacao a pessoa escolheu no menu e qual
 * tipo de treino ela marcou por ultimo. Sao preferencias, nao dados.
 */
class Preferencias(context: Context) {

    // MODE_PRIVATE = so este app le este arquivo. O material antigo passa `0`,
    // que e o mesmo valor, mas com a constante fica legivel.
    private val prefs = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    var ordem: TreinoDbHelper.Ordem
        get() {
            val salvo = prefs.getString(CHAVE_ORDEM, null)
            // Se nunca foi salvo, ou se o valor salvo nao existe mais no enum
            // (aconteceu de renomear entre versoes), cai no padrao.
            return TreinoDbHelper.Ordem.entries.firstOrNull { it.name == salvo }
                ?: TreinoDbHelper.Ordem.MAIS_RECENTE
        }
        set(valor) {
            // apply() grava em segundo plano e retorna na hora.
            // commit() grava na thread atual e devolve true/false — usar commit()
            // na thread principal trava a interface. Prefira apply().
            prefs.edit().putString(CHAVE_ORDEM, valor.name).apply()
        }

    var ultimoTipo: String?
        get() = prefs.getString(CHAVE_ULTIMO_TIPO, null)
        set(valor) {
            prefs.edit().putString(CHAVE_ULTIMO_TIPO, valor).apply()
        }

    private companion object {
        const val ARQUIVO = "preferencias_treino"
        const val CHAVE_ORDEM = "ordem_listagem"
        const val CHAVE_ULTIMO_TIPO = "ultimo_tipo"
    }
}

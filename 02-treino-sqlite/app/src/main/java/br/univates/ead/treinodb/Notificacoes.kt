package br.univates.ead.treinodb

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Tudo que diz respeito a notificacao mora aqui.
 *
 * `object` em Kotlin cria uma instancia unica da classe (singleton). Nao ha
 * `new Notificacoes()`; chama-se `Notificacoes.enviarLembrete(...)` direto.
 *
 * Sao tres pecas, nesta ordem:
 *
 *   1. Canal   — a "categoria" da notificacao. O usuario pode silenciar um
 *                canal sem silenciar o app inteiro. Obrigatorio desde a API 26.
 *   2. Permissao — desde a API 33 o usuario precisa autorizar notificacoes.
 *   3. Notificacao — o aviso em si, entregue ao sistema pelo NotificationManager.
 */
object Notificacoes {

    /** Identificador do canal. Precisa ser o mesmo na criacao e na notificacao. */
    const val ID_CANAL = "lembretes_treino"

    /**
     * Identificador da notificacao. Notificar de novo com o MESMO id substitui
     * a anterior; com um id diferente, empilha mais uma na barra.
     */
    private const val ID_NOTIFICACAO = 1

    /**
     * Cria (ou atualiza) o canal. Pode ser chamado quantas vezes quiser: se o
     * canal ja existe, o sistema ignora.
     *
     * `NotificationChannelCompat` e a versao da androidx: em aparelhos abaixo
     * da API 26 ela simplesmente nao faz nada, e o codigo fica sem `if` de
     * versao espalhado.
     */
    fun criarCanal(context: Context) {
        val canal = NotificationChannelCompat
            .Builder(ID_CANAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.canal_nome))
            .setDescription(context.getString(R.string.canal_descricao))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(canal)
    }

    /**
     * Diz se o app pode notificar agora.
     *
     * Abaixo da API 33 nao existe a permissao, entao a resposta e sempre sim.
     * Da API 33 em diante e preciso consultar o estado real.
     */
    fun temPermissao(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Monta e entrega a notificacao.
     *
     * O `@SuppressLint` esta aqui porque o verificador do Android Studio nao
     * consegue enxergar que [temPermissao] ja fez a checagem logo acima — a
     * permissao ESTA sendo verificada, so nao na linha que ele inspeciona.
     */
    @SuppressLint("MissingPermission")
    fun enviarLembrete(context: Context, titulo: String, texto: String) {
        if (!temPermissao(context)) return

        // O que acontece quando a pessoa toca na notificacao: abre o app.
        // PendingIntent = uma Intent que o SISTEMA vai disparar depois, em nome
        // do nosso app. FLAG_IMMUTABLE e obrigatorio para apps com targetSdk 31
        // ou maior: sem ele, ESTA chamada lanca IllegalArgumentException. E
        // regra de targetSdk, nao da versao do aparelho.
        val intent = Intent(context, MainActivity::class.java)
        val acaoAoTocar = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacao = NotificationCompat.Builder(context, ID_CANAL)
            // O icone pequeno e obrigatorio. Se faltar, nada aparece.
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            // BigTextStyle deixa o texto inteiro visivel quando o usuario
            // expande a notificacao. Sem ele, o texto e cortado numa linha.
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(acaoAoTocar)
            // Some da barra ao ser tocada.
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_NOTIFICACAO, notificacao)
    }
}

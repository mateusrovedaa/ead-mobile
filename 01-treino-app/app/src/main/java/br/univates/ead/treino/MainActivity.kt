package br.univates.ead.treino

// Cada import traz uma classe que vive fora deste arquivo. Nao precisa decorar:
// o Android Studio adiciona sozinho com Alt+Enter em cima do nome em vermelho.
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import br.univates.ead.treino.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Tela unica do app. Reune os componentes da Unidade 2 e registra "treinos" em
 * uma lista que existe apenas na memoria (nada e salvo em disco).
 *
 * `AppCompatActivity` e a classe base de toda tela no Android. E dela que vem o
 * ciclo de vida (onCreate, onStart, onResume, onDestroy...) e a compatibilidade
 * com versoes antigas do sistema.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding: classe gerada pelo Gradle a partir do activity_main.xml, com
    // um campo para cada view que tem @+id no layout.
    // `lateinit` = "prometo preencher antes de usar". E necessario porque o
    // layout so pode ser inflado dentro do onCreate, nao aqui.
    private lateinit var binding: ActivityMainBinding

    // Os dados que alimentam a ListView.
    // `val` mesmo sendo uma lista que muda: o `val` proibe trocar a lista por
    // OUTRA lista; o conteudo dela continua podendo mudar (mutableListOf).
    private val treinos = mutableListOf<String>()

    // O adapter e a ponte entre os dados acima e as linhas desenhadas na tela.
    private lateinit var adapter: ArrayAdapter<String>

    // Quanto tempo ja tinha corrido quando o cronometro foi parado. E o que faz
    // o proximo "Iniciar" continuar de onde parou em vez de voltar a zero.
    private var tempoAcumulado = 0L

    // Impede que dois toques seguidos em "Iniciar" reiniciem a contagem.
    private var cronometroRodando = false

    /**
     * Primeiro metodo do ciclo de vida: roda uma unica vez, quando a tela e
     * criada. E aqui que se monta o layout e se ligam os cliques dos botoes.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Tem que vir ANTES do setContentView. Liga o modo em que o app desenha
        // por baixo da barra de status e da barra de navegacao — comportamento
        // obrigatorio a partir do targetSdk 35.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // inflate = ler o XML do layout e criar os objetos de View de verdade.
        binding = ActivityMainBinding.inflate(layoutInflater)
        // binding.root e o LinearLayout mais externo do activity_main.xml.
        setContentView(binding.root)

        // Consequencia do edge-to-edge: o conteudo comeca colado no topo fisico
        // da tela. Aqui o sistema informa o tamanho das barras e nos afastamos o
        // conteudo com padding — sem isto o titulo fica atras do relogio.
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { view, insets ->
            val barras = insets.getInsets(
                // systemBars = barra de status + barra de navegacao.
                // ime = o teclado, para o conteudo subir quando ele abre.
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime(),
            )
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        // simple_list_item_1 e um layout de linha que ja vem pronto no proprio
        // Android: um TextView simples. Repare no `android.R`, com ponto — e o
        // R do SISTEMA, diferente do R do nosso app.
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, treinos)
        binding.listaTreinos.adapter = adapter

        configurarCronometro()

        // setOnClickListener registra o que fazer QUANDO o botao for tocado.
        // O codigo entre chaves nao roda agora; fica guardado esperando o toque.
        binding.botaoRegistrar.setOnClickListener { registrarTreino() }

        atualizarDiagnostico()

        Log.d("CICLO", "onCreate")
    }

    // Os metodos abaixo existem so para deixar o ciclo de vida visivel no
    // Logcat. Filtre por `tag:CICLO` e observe a sequencia ao girar a tela, ao
    // apertar Home e ao voltar para o app. Repare que voltar para o app NAO
    // passa pelo onCreate.
    override fun onStart() {
        super.onStart()
        Log.d("CICLO", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CICLO", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CICLO", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CICLO", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CICLO", "onDestroy")
    }

    /**
     * Liga os tres botoes do cronometro.
     *
     * A ideia central do [android.widget.Chronometer]: ele nao guarda "quanto
     * tempo passou", ele guarda "QUANDO comecou" (a base) e desenha na tela a
     * diferenca ate agora. Todo o resto sai dai.
     */
    private fun configurarCronometro() {
        zerarCronometro()

        binding.botaoIniciar.setOnClickListener {
            // Ja esta rodando: ignora o toque, senao a contagem reiniciaria.
            if (cronometroRodando) return@setOnClickListener

            // Para retomar de onde parou, joga-se a base PARA TRAS pelo tempo ja
            // acumulado. Sem esta subtracao, todo Iniciar recomeca do zero — e o
            // bug que todo mundo escreve na primeira vez.
            binding.cronometro.base = SystemClock.elapsedRealtime() - tempoAcumulado
            binding.cronometro.start()
            cronometroRodando = true
        }

        binding.botaoParar.setOnClickListener {
            if (!cronometroRodando) return@setOnClickListener
            binding.cronometro.stop()
            // Guarda quanto ja correu, para o proximo Iniciar continuar daqui.
            tempoAcumulado = SystemClock.elapsedRealtime() - binding.cronometro.base
            cronometroRodando = false
        }

        binding.botaoZerar.setOnClickListener { zerarCronometro() }
    }

    /** Volta o cronometro para 00:00 e esquece o tempo acumulado. */
    private fun zerarCronometro() {
        binding.cronometro.stop()
        // elapsedRealtime = tempo desde que o aparelho ligou. Usa-se ele, e nao
        // a hora do relogio, porque o usuario pode mudar o fuso ou a hora do
        // sistema no meio da contagem e baguncar o calculo.
        binding.cronometro.base = SystemClock.elapsedRealtime()
        tempoAcumulado = 0L
        cronometroRodando = false
    }

    /**
     * Le todos os componentes da tela, monta uma linha de texto e coloca no topo
     * da lista. Nada e salvo: fechando o app, os registros somem.
     */
    private fun registrarTreino() {
        // .text devolve um Editable; .toString() converte para String comum.
        // .trim() remove espacos sobrando nas pontas do que foi digitado.
        val atividade = binding.campoAtividade.text.toString().trim()
        if (atividade.isEmpty()) {
            // setError faz o proprio EditText mostrar o balaozinho vermelho.
            binding.campoAtividade.error = getString(R.string.informe_atividade)
            return
        }

        // Um RadioGroup sem nenhuma opcao marcada devolve View.NO_ID. E assim
        // que se testa "o usuario ainda nao escolheu".
        if (binding.grupoTipo.checkedRadioButtonId == View.NO_ID) {
            // O RadioGroup nao tem setError proprio, entao o aviso vai para a
            // Snackbar (a tarja preta que sobe no rodape).
            Snackbar.make(binding.raiz, R.string.escolha_tipo, Snackbar.LENGTH_LONG).show()
            return
        }
        // checkedRadioButtonId devolve o ID; findViewById troca o ID pela view,
        // e dela lemos o texto ("Cardio", "Forca" ou "Alongamento").
        val tipo = findViewById<RadioButton>(binding.grupoTipo.checkedRadioButtonId).text

        val picker = binding.seletorData
        // ATENCAO ao `+ 1`: o DatePicker conta os meses A PARTIR DE ZERO
        // (janeiro = 0). Esquecer isso e classico e o erro passa despercebido.
        // %02d = numero com 2 digitos, completando com zero a esquerda.
        val data = "%02d/%02d/%04d".format(picker.dayOfMonth, picker.month + 1, picker.year)

        // isChecked vale para CheckBox e RadioButton: diz se esta marcado.
        val aquecimento = if (binding.caixaAquecimento.isChecked) {
            getString(R.string.sufixo_aquecimento)
        } else {
            ""
        }

        // getString com argumentos preenche os %1$s, %2$s... da string do XML.
        // add(0, ...) insere na POSICAO 0, ou seja, no topo da lista.
        treinos.add(0, getString(R.string.linha_treino, atividade, tipo, data, aquecimento))

        // Sem esta linha a ListView continua desenhando a lista antiga: mexer na
        // lista nao avisa ninguem, o adapter precisa ser notificado.
        adapter.notifyDataSetChanged()

        // Esconde o "Nenhum treino registrado ainda." a partir do primeiro item.
        binding.textoVazio.isVisible = treinos.isEmpty()
        // Limpa o campo para o proximo registro.
        binding.campoAtividade.text.clear()
        atualizarDiagnostico()
        Snackbar.make(binding.raiz, R.string.treino_registrado, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Escreve na tela qual objeto Activity esta desenhando e quantos treinos ele
     * tem na memoria.
     *
     * `hashCode()` identifica ESTA instancia. Ao girar a tela o numero muda: e
     * outro objeto. Ao voltar do Home o numero se mantem: e o mesmo objeto.
     */
    private fun atualizarDiagnostico() {
        binding.textoDiagnostico.text = getString(
            R.string.diagnostico,
            Integer.toHexString(hashCode()),
            treinos.size,
        )
    }
}

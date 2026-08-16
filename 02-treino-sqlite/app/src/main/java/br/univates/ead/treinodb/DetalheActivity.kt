package br.univates.ead.treinodb

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.univates.ead.treinodb.databinding.ActivityDetalheBinding

/**
 * Segunda tela: mostra um treino da lista.
 *
 * Repare no que NAO tem aqui: nenhuma consulta ao banco. Os dados chegaram
 * prontos dentro da Intent. Para um registro pequeno isso e o suficiente; para
 * algo maior, o padrao e mandar so o id e deixar esta tela buscar o resto.
 */
class DetalheActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalheBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setPadding substitui o padding do XML, nao soma. Dai a margem vir de
        // um recurso e ser somada ao recuo das barras do sistema.
        val margem = resources.getDimensionPixelSize(R.dimen.espaco_tela)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                barras.left + margem,
                barras.top + margem,
                barras.right + margem,
                barras.bottom + margem,
            )
            insets
        }

        // Do outro lado do envelope: le-se pelo mesmo nome usado no putExtra.
        // Se a chave nao existir, getStringExtra devolve null — dai o "?: ...".
        val atividade = intent.getStringExtra(EXTRA_ATIVIDADE).orEmpty()
        val tipo = intent.getStringExtra(EXTRA_TIPO).orEmpty()
        val data = intent.getStringExtra(EXTRA_DATA).orEmpty()
        // Para booleano e obrigatorio informar o valor padrao.
        val aquecimento = intent.getBooleanExtra(EXTRA_AQUECIMENTO, false)

        binding.detalheAtividade.text = atividade
        binding.detalheTipo.text = getString(R.string.detalhe_tipo, tipo)
        binding.detalheData.text = getString(R.string.detalhe_data, data)
        binding.detalheAquecimento.text = getString(
            R.string.detalhe_aquecimento,
            getString(if (aquecimento) R.string.sim else R.string.nao),
        )

        // finish() encerra ESTA tela e volta para a anterior, que continuava
        // viva na pilha. Chamar startActivity(MainActivity) aqui seria errado:
        // criaria uma segunda copia da tela inicial empilhada sobre esta.
        binding.botaoVoltar.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_ATIVIDADE = "atividade"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_DATA = "data"
        const val EXTRA_AQUECIMENTO = "aquecimento"
    }
}

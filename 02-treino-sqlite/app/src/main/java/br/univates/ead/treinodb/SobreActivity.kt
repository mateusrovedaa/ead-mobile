package br.univates.ead.treinodb

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.univates.ead.treinodb.databinding.ActivitySobreBinding

/**
 * Tela aberta pelo item "Sobre" do menu.
 *
 * Ela mostra o caminho real do arquivo do banco dentro do aparelho. Serve para
 * deixar concreto que o SQLite nao e uma abstracao: e um arquivo, com endereco,
 * numa pasta privada do app.
 */
class SobreActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySobreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySobreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Atencao: setPadding SUBSTITUI o padding declarado no XML, nao soma.
        // Por isso o respiro da tela vem de um recurso e e somado aqui — se
        // ficasse no layout, seria apagado na primeira vez que este bloco rodar.
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

        // getDatabasePath devolve o caminho completo do arquivo .db.
        binding.textoCaminhoBanco.text = getDatabasePath(TreinoDbHelper.NOME_BANCO).absolutePath

        binding.botaoVoltar.setOnClickListener { finish() }
    }
}

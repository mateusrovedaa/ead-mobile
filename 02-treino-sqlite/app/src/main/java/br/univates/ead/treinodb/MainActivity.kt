package br.univates.ead.treinodb

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import br.univates.ead.treinodb.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Tela principal.
 *
 * A diferenca para o app da Unidade 2 e uma so, mas muda tudo: a lista nao vive
 * mais numa variavel da Activity. Ela vive num arquivo de banco de dados dentro
 * do aparelho. Por isso ela sobrevive a girar a tela, a fechar o app e ate a
 * reiniciar o celular.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** A porta de entrada do banco. Uma instancia por Activity basta. */
    private lateinit var bd: TreinoDbHelper

    /** As preferencias do usuario (ordenacao escolhida, ultimo tipo marcado). */
    private lateinit var prefs: Preferencias

    /**
     * A lista que esta na tela AGORA. Nao e a fonte da verdade — e uma copia do
     * que o banco devolveu na ultima consulta. Serve para saber, ao tocar numa
     * linha, qual treino foi tocado.
     */
    private var treinos: List<Treino> = emptyList()

    /**
     * Quantos treinos ESTA instancia da Activity registrou.
     *
     * E um campo comum, como qualquer outro. Serve para a aula: ao girar a tela
     * ele volta a zero, porque a instancia e outra — enquanto o total do banco
     * continua igual.
     */
    private var gravadosNestaTela = 0

    private lateinit var adapter: ArrayAdapter<String>

    /**
     * O pedido de permissao de notificacao.
     *
     * Isto substitui o antigo `onRequestPermissionsResult`: em vez de receber a
     * resposta num metodo separado, com um codigo numerico para saber de qual
     * pedido se tratava, registra-se aqui o que fazer com a resposta.
     *
     * Precisa ser registrado como campo da classe, e nao dentro de um clique:
     * o registro tem que acontecer antes da tela ficar visivel.
     */
    private val pedidoDePermissao = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        if (concedida) {
            enviarLembrete()
        } else {
            Snackbar.make(binding.raiz, R.string.permissao_negada, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { view, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime(),
            )
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        // A Toolbar do layout passa a valer como barra de acoes da Activity.
        // Sem esta linha o menu existe, mas nao tem onde aparecer.
        setSupportActionBar(binding.barraSuperior)

        bd = TreinoDbHelper(this)
        prefs = Preferencias(this)

        // O canal precisa existir ANTES da primeira notificacao. Criar no
        // onCreate da tela inicial e o lugar mais simples.
        Notificacoes.criarCanal(this)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.listaTreinos.adapter = adapter

        configurarMenu()
        restaurarUltimoTipo()

        binding.botaoRegistrar.setOnClickListener { registrarTreino() }
        binding.botaoLembrete.setOnClickListener { pedirPermissaoEEnviar() }

        // Tocar numa linha da lista abre a segunda tela com aquele treino.
        binding.listaTreinos.setOnItemClickListener { _, _, posicao, _ ->
            abrirDetalhe(treinos[posicao])
        }

        // O hashCode identifica ESTE objeto Activity na memoria. Ao girar a tela
        // o numero muda: prova de que nao e a mesma Activity, e sim uma nova.
        // Voltar do Home mantem o numero — a mesma Activity continuava viva.
        Log.d(CICLO, "onCreate  — instancia ${hashCode()}")
    }

    /**
     * Recarrega a lista toda vez que a tela volta a ficar visivel.
     *
     * Por que no onStart e nao no onCreate? Porque ao voltar da tela de detalhe
     * o onCreate NAO roda de novo — a Activity nao foi recriada, so estava
     * parada. Consulta de banco costuma ir no onStart ou no onResume.
     */
    override fun onStart() {
        super.onStart()
        Log.d(CICLO, "onStart    — instancia ${hashCode()}, ${treinos.size} treinos na tela")
        carregarLista()
    }

    // Os tres metodos abaixo existem so para deixar o ciclo de vida visivel no
    // Logcat. Filtre por `tag:CICLO` e observe a sequencia ao girar a tela, ao
    // apertar Home e ao voltar para o app.
    override fun onResume() {
        super.onResume()
        Log.d(CICLO, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(CICLO, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(CICLO, "onStop")
    }

    /** Fecha a conexao com o banco quando a tela e destruida de vez. */
    override fun onDestroy() {
        Log.d(CICLO, "onDestroy")
        bd.close()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- banco

    /**
     * Le o banco e joga o resultado na ListView.
     *
     * Um aviso honesto: esta consulta roda na thread principal, a mesma que
     * desenha a tela. Com poucas dezenas de linhas ninguem percebe. Com
     * milhares, a interface trava — e a partir dai a resposta certa deixa de
     * ser SQLite na mao e passa a ser Room com corrotinas, que empurra a
     * consulta para fora da thread principal.
     */
    private fun carregarLista() {
        treinos = bd.listar(prefs.ordem)

        val linhas = treinos.map { treino ->
            val sufixo = if (treino.aquecimento) getString(R.string.sufixo_aquecimento) else ""
            getString(R.string.linha_treino, treino.atividade, treino.tipo, treino.data, sufixo)
        }

        // O adapter e a ponte entre os dados e as linhas desenhadas. Trocar o
        // conteudo dele exige avisar: sem o notifyDataSetChanged a ListView
        // continua mostrando o que estava antes.
        adapter.clear()
        adapter.addAll(linhas)
        adapter.notifyDataSetChanged()

        binding.textoVazio.isVisible = treinos.isEmpty()

        // Linha de diagnostico da aula. `hashCode()` identifica ESTA instancia da
        // Activity: ao girar a tela o numero muda, porque e outro objeto — e por
        // isso `gravadosNestaTela` volta a zero. O contador do banco nao se abala.
        binding.textoTotal.text = getString(
            R.string.total_treinos,
            Integer.toHexString(hashCode()),
            gravadosNestaTela,
            bd.contar(),
        )
    }

    /** Le a tela, valida e grava uma linha nova no banco. */
    private fun registrarTreino() {
        val atividade = binding.campoAtividade.text.toString().trim()
        if (atividade.isEmpty()) {
            binding.campoAtividade.error = getString(R.string.informe_atividade)
            return
        }

        if (binding.grupoTipo.checkedRadioButtonId == View.NO_ID) {
            Snackbar.make(binding.raiz, R.string.escolha_tipo, Snackbar.LENGTH_LONG).show()
            return
        }
        val tipo = findViewById<RadioButton>(binding.grupoTipo.checkedRadioButtonId)
            .text
            .toString()

        val picker = binding.seletorData
        // O DatePicker conta os meses a partir de zero: janeiro e 0. Dai o + 1.
        val data = "%02d/%02d/%04d".format(picker.dayOfMonth, picker.month + 1, picker.year)

        val aquecimento = binding.caixaAquecimento.isChecked

        // insert devolve o id da linha criada, ou -1 se nao deu certo.
        val id = bd.inserir(atividade, tipo, data, aquecimento)
        if (id == -1L) {
            Snackbar.make(binding.raiz, R.string.falha_registro, Snackbar.LENGTH_LONG).show()
            return
        }

        gravadosNestaTela++

        // Guarda o tipo escolhido para ja vir marcado na proxima abertura.
        prefs.ultimoTipo = tipo

        binding.campoAtividade.text.clear()
        carregarLista()
        Snackbar.make(binding.raiz, R.string.treino_registrado, Snackbar.LENGTH_SHORT).show()
    }

    /** Marca o RadioButton do tipo usado da ultima vez, se houver. */
    private fun restaurarUltimoTipo() {
        val salvo = prefs.ultimoTipo ?: return
        val botoes = listOf(binding.tipoCardio, binding.tipoForca, binding.tipoAlongamento)
        botoes.firstOrNull { it.text.toString() == salvo }?.isChecked = true
    }

    // ----------------------------------------------------------------- menu

    /**
     * Liga o menu da Toolbar.
     *
     * O material da unidade usa `onCreateOptionsMenu` e `onOptionsItemSelected`
     * sobrescritos na Activity. Continua funcionando. O caminho recomendado hoje
     * e este [MenuProvider]: ele recebe o ciclo de vida como argumento, entao o
     * menu se registra e se desregistra sozinho junto com a tela. Isso resolve o
     * problema antigo de menus duplicados ou vazando entre telas.
     */
    private fun configurarMenu() {
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // "Inflar" = ler o XML e criar os objetos de menu, o mesmo
                    // verbo usado com layouts.
                    menuInflater.inflate(R.menu.menu_principal, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // Nao se usa `when` com R.id.* aqui: desde o Gradle 8 esses
                    // identificadores deixaram de ser constantes, entao a
                    // comparacao vai em if/else if.
                    val id = menuItem.itemId
                    return when {
                        id == R.id.acao_ordenar_recente -> {
                            aplicarOrdem(TreinoDbHelper.Ordem.MAIS_RECENTE)
                            true
                        }
                        id == R.id.acao_ordenar_atividade -> {
                            aplicarOrdem(TreinoDbHelper.Ordem.ATIVIDADE)
                            true
                        }
                        id == R.id.acao_limpar -> {
                            confirmarLimpeza()
                            true
                        }
                        id == R.id.acao_sobre -> {
                            startActivity(Intent(this@MainActivity, SobreActivity::class.java))
                            true
                        }
                        // false = "nao tratei este item", deixa outro tratar.
                        else -> false
                    }
                }
            },
            // Ao passar o lifecycleOwner, o menu some sozinho quando a tela morre.
            this,
        )
    }

    private fun aplicarOrdem(ordem: TreinoDbHelper.Ordem) {
        prefs.ordem = ordem
        carregarLista()
    }

    /**
     * Apagar dados sem perguntar e uma das coisas que mais irritam usuario.
     * O AlertDialog do Material da a chance de desistir.
     */
    private fun confirmarLimpeza() {
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_limpar)
            .setMessage(R.string.confirmar_limpeza)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.limpar) { _, _ ->
                bd.excluirTodos()
                carregarLista()
            }
            .show()
    }

    // -------------------------------------------------------------- telas

    /**
     * Abre a segunda tela levando os dados do treino junto.
     *
     * A Intent e ao mesmo tempo o "quero abrir aquela tela" e o envelope com os
     * dados. Cada `putExtra` e um par nome/valor que a outra Activity le pelo
     * mesmo nome. Nao ha construtor: uma Activity nunca e criada com `new`.
     */
    private fun abrirDetalhe(treino: Treino) {
        val intent = Intent(this, DetalheActivity::class.java).apply {
            putExtra(DetalheActivity.EXTRA_ATIVIDADE, treino.atividade)
            putExtra(DetalheActivity.EXTRA_TIPO, treino.tipo)
            putExtra(DetalheActivity.EXTRA_DATA, treino.data)
            putExtra(DetalheActivity.EXTRA_AQUECIMENTO, treino.aquecimento)
        }
        startActivity(intent)
    }

    // -------------------------------------------------------- notificacao

    /**
     * Da API 33 em diante, notificar exige autorizacao do usuario.
     *
     * O fluxo tem tres caminhos: ja tenho permissao, entao envio; a versao do
     * Android e antiga e nao precisa de permissao, entao envio; ou preciso
     * pedir, e so envio se a resposta vier positiva.
     */
    private fun pedirPermissaoEEnviar() {
        if (Notificacoes.temPermissao(this)) {
            enviarLembrete()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedidoDePermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun enviarLembrete() {
        Notificacoes.enviarLembrete(
            context = this,
            titulo = getString(R.string.lembrete_titulo),
            texto = getString(R.string.lembrete_texto, bd.contar()),
        )
    }

    private companion object {
        /** Etiqueta usada nos logs de ciclo de vida. No Logcat: `tag:CICLO`. */
        const val CICLO = "CICLO"
    }
}

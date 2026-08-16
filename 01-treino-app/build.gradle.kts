// Build de topo: apenas declara os plugins que os modulos vao aplicar.
// `apply false` = carrega a versao no classpath sem ativar no projeto raiz.
plugins {
    alias(libs.plugins.android.application) apply false
}

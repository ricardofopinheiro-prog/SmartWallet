# SmartWallet

Projeto Android nativo em Kotlin + Jetpack Compose, preparado para abrir no Android Studio.

## Requisitos
- Android Studio atualizado (recomendado Ladybug ou mais recente)
- JDK 17
- Android SDK 35

## Como abrir
1. Extrair o ZIP.
2. Abrir a pasta `SmartWallet` no Android Studio.
3. Aguardar a sincronização do Gradle.
4. Ligar um telemóvel Android com depuração USB ou criar um emulador.
5. Carregar em **Run**.

## O que esta primeira versão já inclui
- Dashboard com saldo, rendimentos e despesas do mês.
- Registo de rendimentos e despesas.
- Categorias e descrição dos movimentos.
- Histórico de movimentos.
- Persistência local com Room (os dados ficam no dispositivo).
- Cálculos monetários em cêntimos (`Long`), evitando erros típicos de `Float`/`Double`.

## Limitações desta primeira entrega
Esta é a base funcional inicial, não a versão final de todas as funcionalidades do documento. Ainda faltam módulos como contas múltiplas, transferências, objetivos de poupança, créditos, orçamentos, notificações, exportação, cópias de segurança e testes abrangentes. Estes devem ser acrescentados e testados por etapas.

## APK
O APK não está incluído nesta entrega porque precisa de ser compilado com o Android SDK/Gradle no Android Studio. Para gerar:
`Build > Build Bundle(s) / APK(s) > Build APK(s)`

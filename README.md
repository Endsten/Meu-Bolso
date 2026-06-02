# Meu Bolso - Gestor de Gastos Pessoais

O **Meu Bolso** é um aplicativo Android moderno para gestão de finanças pessoais, desenvolvido com foco em simplicidade, segurança e visualização clara de dados.

## 🚀 Funcionalidades

### 1. Gestão de Gastos
*   **Lançamento Rápido**: Adicione gastos informando descrição, valor, categoria e data.
*   **Edição e Exclusão**: Gerencie seus lançamentos facilmente através da lista de "Últimos Lançamentos".
*   **Categorização**: Organize seus gastos em categorias como Alimentação, Transporte, Moradia, Saúde, Lazer, etc.

### 2. Dashboard e Relatórios
*   **Visão Geral**: Dashboard com cards de Saldo e Total de Gastos.
*   **Análise por Categoria**: Gráficos de barras que mostram o percentual de gastos por categoria.
*   **Filtros Temporais**: Visualize seus gastos por Semana, Mês ou período Total.
*   **Exportação**: Exporte seus relatórios financeiros para o formato CSV (compatível com Excel).

### 3. Segurança e Perfil
*   **Autenticação**: Sistema de login e cadastro de usuários.
*   **Biometria**: Suporte a login por impressão digital (BiometricPrompt).
*   **Controle de Renda**: Configure seu salário mensal no perfil para acompanhar o quanto da sua renda está sendo consumida.
*   **Sessão Persistente**: Opção de "Permanecer logado" utilizando DataStore.

## 🛠 Tecnologias Utilizadas

*   **Linguagem**: [Kotlin](https://kotlinlang.org/)
*   **Interface (UI)**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Arquitetura declarativa)
*   **Banco de Dados**: [Room](https://developer.android.com/training/data-storage/room) (Persistência local)
*   **Processamento de Código**: [KSP](https://kotlinlang.org/docs/ksp-overview.html) (Kotlin Symbol Processing)
*   **Arquitetura**: MVVM (ViewModel, StateFlow)
*   **Segurança**: Biometric API
*   **Armazenamento de Preferências**: Jetpack DataStore
*   **Injeção de Dependências**: Manual (via Factory/ViewModel)

## 📁 Estrutura do Projeto

*   `MainActivity.kt`: Gerencia a navegação e todas as telas da interface (Compose).
*   `GastoViewModel.kt`: Concentra a lógica de negócio e estados da UI.
*   `data/`:
    *   `AppDatabase.kt`: Configuração central do banco de dados SQLite.
    *   `Gastos.kt` & `Usuario.kt`: Entidades de dados.
    *   `GastoDao.kt` & `UsuarioDao.kt`: Interfaces de acesso ao banco de dados.
    *   `SessionManager.kt`: Gerenciamento da sessão do usuário com DataStore.

## ⚙️ Requisitos para Compilação

*   Android Studio Ladybug ou superior.
*   JDK 17.
*   Gradle 8.10.2.
*   SDK Mínimo: API 24 (Android 7.0).
*   SDK Alvo: API 35 (Android 15).

---
*Desenvolvido como um gestor financeiro prático e seguro.*

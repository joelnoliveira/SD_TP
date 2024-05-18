# Googol
## Dependências
- É necessário um terminal Linux para correr os bash scripts.
- Pode ser que seja necessário fazer chmod dos scripts para que sejam possíveis de executar.
- As dependências do Maven estão contidas no [pom.xml](./web_server/pom.xml).
- Fora as dependências do Maven, o projeto precisa também de um jar do JSOUP ([jsoup-1.17.2.jar](./not_web_server/lib/jars/jsoup-1.17.2.jar)).

## Compilar

- Para compilar todos os ficheiros basta correr o script [compile_all.sh](./scripts/compile_all.sh), exemplo a partir da raíz do projeto "./scripts/compile_all.sh". Este script irá gerar também um jar que será necessário para o Maven correr.
- Este pode ser executado juntamente com um dos seguintes argumentos:
    - "-delete_bin_before_compile", apaga todo o conteúdo da pasta [bin](./bin), que contém todos os ficheiros binários, e depois compila.
    - "-delete_logs", apaga todos os ficheiros output de texto situados na pasta [logs](./logs) e depois compila.
    - "-clean_all", as duas opções anteriores combinadas.

## Execução
### Ficheiros [downloader_runner.sh](./scripts/downloader_runner.sh), [index_storage_barrel_runner.sh](./scripts/index_storage_barrel_runner.sh)
- Ambos seguem a mesma base de execução.
- Exemplo da execução a partir da raíz do projeto: "./scripts/index_storage_barrel_runner.sh", sendo que neste caso será escolhido automáticamente um ID que corresponde ao número de nanosegundos dos sistema (é difícil mas não impossível que haja dois IDs iguais) e ficheiros de input e output default definidos no script.
- Caso se prenteda alterar o ID, poderá executar-se da seguinte forma: "./scripts/downloader_runner.sh 1" onde "1" é o ID.
- Ou então caso se queira alterar o ficheiro input de configuração (.config) e o ficheiro de output, deverá executar-se o script da seguinte forma: "./scripts/downloader_runner.sh 1 path_ficheiro.config path_ficheiro_output", onde 1 é o ID.

### Ficheiro [rmi_gateway_runner.sh](./scripts/rmi_gateway_runner.sh)
- A execução é idêntica à anteriormente referida sendo que a única coisa que difere é que a RMIGateway não tem ID.

### Ficheiro [client_runner.sh](./scripts/client_runner.sh)
- A execução é idêntica à dos ficheiros [downloader_runner.sh](./scripts/downloader_runner.sh) e [index_storage_barrel_runner.sh](./scripts/index_storage_barrel_runner.sh), à exceção que este não necessita de um ficheiro para o output.

## Estrutura do projeto
```
├── bin
├── config
├── docs
│   └── javadoc
│       ├── com
│       │   └── ds
│       │       └── googol
│       │           ├── controller
│       │           └── model
│       ├── legal
│       ├── meta1
│       ├── resources
│       └── script-dir
├── logs
├── not_web_server
│   ├── lib
│   │   └── jars
│   └── src
│       └── meta1
├── scripts
└── web_server
    ├── lib
    │   └── jars
    ├── src
    │   ├── main
    │   │   ├── java
    │   │   │   └── com
    │   │   │       └── ds
    │   │   │           └── googol
    │   │   │               ├── controller
    │   │   │               └── model
    │   │   └── resources
    │   │       ├── static
    │   │       └── templates
    │   └── test
    │       └── java
    │           └── com
    │               └── ds
    │                   └── googol
    └── target


```

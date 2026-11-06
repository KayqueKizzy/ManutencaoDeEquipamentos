# Manutenção de Equipamentos

Aplicação web para cadastrar equipamentos, acompanhar sua situação e registrar chamados de manutenção. O backend usa Java com Spring Boot e salva os dados no OrientDB.

## Requisitos

- JDK 11
- Maven 3.6 ou superior
- OrientDB Community 3.2.57 em execução no computador

O OrientDB 3.2 documenta suporte a Java 8 até 11. Por isso, este projeto usa Java 11 e Spring Boot 2.7.

## 1. Inicie o OrientDB no Windows

Extraia o OrientDB. Abra a pasta `bin` e execute `server.bat`. Na primeira execução, defina a senha do usuário `root` do servidor. Mantenha a janela aberta.

Abra `http://localhost:2480` no navegador para acessar o OrientDB Studio.

## 2. Crie o banco da aplicação

No OrientDB Studio, crie um banco chamado `manutencaoequipamentos`, com armazenamento local (`PLOCAL`). Configure um usuário do banco com permissão de administrador. O usuário e a senha usados no projeto devem corresponder aos definidos para esse banco.

Por padrão, o projeto espera:

- Banco: `manutencaoequipamentos`
- Usuário do banco: `admin`
- Senha do banco: `admin123`
- Servidor: `remote:localhost`

Para usar outros valores, ajuste `src/main/resources/application.properties` ou defina as variáveis `ORIENTDB_SERVER_URL`, `ORIENTDB_DATABASE`, `ORIENTDB_USERNAME` e `ORIENTDB_PASSWORD` antes de iniciar a aplicação.

Na primeira inicialização do projeto, as classes de documentos `Equipment` e `Maintenance` e seus campos são criados automaticamente no banco.

## 3. Inicie a aplicação

Abra o terminal nesta pasta e execute:

```powershell
mvn spring-boot:run
```

Depois acesse `http://localhost:8080`.

## Funcionalidades

- Cadastrar, consultar, editar e excluir equipamentos.
- Pesquisar e filtrar equipamentos por situação.
- Abrir chamados de manutenção com prioridade e descrição.
- Marcar chamados como concluídos.
- Atualizar automaticamente a situação do equipamento ao abrir ou concluir chamados.
- Preservar o histórico dos chamados quando um equipamento é excluído.

## Endpoints da API

| Método | Caminho | Ação |
|---|---|---|
| GET | `/api/equipamentos` | Lista equipamentos |
| GET | `/api/equipamentos/{id}` | Consulta um equipamento |
| POST | `/api/equipamentos` | Cadastra um equipamento |
| PUT | `/api/equipamentos/{id}` | Atualiza um equipamento |
| DELETE | `/api/equipamentos/{id}` | Exclui um equipamento |
| GET | `/api/manutencoes` | Lista chamados |
| GET | `/api/manutencoes?equipamentoId={id}` | Lista chamados de um equipamento |
| POST | `/api/manutencoes/equipamento/{id}` | Abre chamado para um equipamento |
| POST | `/api/manutencoes/{id}/concluir` | Conclui um chamado |

## Estrutura dos dados

O OrientDB armazena cada equipamento como um documento da classe `Equipment`. Cada chamado é um documento da classe `Maintenance` com o identificador do equipamento relacionado. Isso permite consultar e atualizar os registros separadamente e manter o histórico de manutenção.

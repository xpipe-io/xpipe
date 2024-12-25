# XPipe Git Vault

XPipe pode sincronizar todos os teus dados de conexão com o teu próprio repositório remoto git. Podes sincronizar com este repositório em todas as instâncias da aplicação XPipe da mesma forma, cada alteração que fizeres numa instância será reflectida no repositório.

Antes de mais, tens de criar um repositório remoto com o teu fornecedor git preferido. Este repositório tem de ser privado.
Depois, basta copiar e colar o URL na definição do repositório remoto do XPipe.

Também precisas de ter um cliente `git` instalado localmente na tua máquina local. Podes tentar executar o `git` num terminal local para verificar.
Se não tiveres um, podes visitar [https://git-scm.com](https://git-scm.com/) para instalar o git.

## Autenticando para o repositório remoto

Existem várias formas de te autenticares. A maioria dos repositórios usa HTTPS onde tens de especificar um nome de utilizador e uma palavra-passe.
Alguns provedores também suportam o protocolo SSH, que também é suportado pelo XPipe.
Se usas o SSH para o git, provavelmente sabes como o configurar, por isso esta secção irá cobrir apenas o HTTPS.

Precisas de configurar o teu git CLI para ser capaz de autenticar com o teu repositório git remoto via HTTPS. Há várias maneiras de fazer isso.
Podes verificar se isso já foi feito reiniciando o XPipe quando um repositório remoto estiver configurado.
Se ele te pedir as tuas credenciais de login, tens de as configurar.

Muitas ferramentas especiais como esta [GitHub CLI](https://cli.github.com/) fazem tudo automaticamente para ti quando instaladas.
Algumas versões mais recentes do cliente git também podem autenticar através de serviços web especiais onde apenas tens de iniciar sessão na tua conta no teu browser.

Existem também formas manuais de te autenticares através de um nome de utilizador e de um token.
Atualmente, a maioria dos fornecedores exige um token de acesso pessoal (PAT) para autenticar a partir da linha de comandos em vez das palavras-passe tradicionais.
Podes encontrar páginas comuns (PAT) aqui:
- **GitHub**: [Tokens de acesso pessoal (clássico)](https://github.com/settings/tokens)
- **GitLab**: [Token de acesso pessoal](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Token de acesso pessoal](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Configurações -> Aplicações -> secção Gerir Tokens de Acesso`
Define a permissão do token para o repositório como Leitura e Escrita. O resto das permissões do token podem ser definidas como Read.
Mesmo que o teu cliente git te peça uma palavra-passe, deves introduzir o teu token, a menos que o teu fornecedor ainda utilize palavras-passe.
- A maioria dos provedores não suporta mais senhas.

Se não quiseres introduzir as tuas credenciais todas as vezes, podes usar qualquer gestor de credenciais git para isso.
Para mais informações, vê por exemplo:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Alguns clientes git modernos também se encarregam de armazenar as credenciais automaticamente.

Se tudo funcionar bem, o XPipe deve enviar um commit para o teu repositório remoto.

## Adicionando categorias ao repositório

Por padrão, nenhuma categoria de conexão é definida para sincronizar, para que tenhas controle explícito sobre quais conexões devem ser confirmadas.
Assim, no início, o teu repositório remoto estará vazio.

Para que as tuas ligações de uma categoria sejam colocadas no teu repositório git,
precisas clicar no ícone de engrenagem (ao passar o mouse sobre a categoria)
no teu separador `Conexões` sob a visão geral da categoria no lado esquerdo.
Em seguida, clica em `Adicionar ao repositório git` para sincronizar a categoria e as conexões ao seu repositório git.
Isso adicionará todas as conexões sincronizáveis ao repositório git.

## As conexões locais não são sincronizadas

Qualquer conexão localizada na máquina local não pode ser compartilhada, pois se refere a conexões e dados que estão disponíveis apenas no sistema local.

Certas conexões que são baseadas em um arquivo local, por exemplo, configurações SSH, podem ser compartilhadas via git se os dados subjacentes, neste caso o arquivo, tiverem sido adicionados ao repositório git também.

## Adicionando arquivos ao git

Quando tudo estiver configurado, tens a opção de adicionar quaisquer ficheiros adicionais, tais como chaves SSH, ao git também.
Junto a cada escolha de ficheiro está um botão git que irá adicionar o ficheiro ao repositório git.
Estes ficheiros são também encriptados quando enviados.

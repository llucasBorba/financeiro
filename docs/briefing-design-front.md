# Briefing de design — Financeiro

Preciso do design de uma aplicação web de finanças pessoais, em português do Brasil.
**São 11 telas.** Não quero código: quero o design das telas.

---

## O produto

Um app onde **uma pessoa** controla o próprio dinheiro. Não é ferramenta de time, não tem
compartilhamento, não tem perfis. Cada usuário vê apenas os próprios dados.

O usuário registra o que vai gastar (contas a pagar) e o que recebeu, marca as contas como
pagas conforme vão sendo quitadas, e acompanha se está gastando mais do que pretendia.

### O conceito central que o design precisa acertar

Existe uma diferença entre **o que já foi pago** e **o que ainda está por pagar**, e ela
aparece em quase todas as telas. Não são a mesma informação e nunca devem ser somadas num
número só:

- **Pago** — o dinheiro já saiu da conta neste mês.
- **A pagar** — a conta existe, vence neste mês, e ainda não foi quitada.
- **Atrasado** — subconjunto do "a pagar": venceu em mês anterior e continua em aberto.
  Uma conta de agosto não paga continua aparecendo em setembro.

O saldo do mês considera apenas o que entrou e o que **saiu de fato**. O que está a pagar
aparece separado, como aviso do que ainda vai sair.

Isso significa que uma despesa tem dois estados visuais bem distintos — paga e pendente — e
o design precisa deixar isso óbvio à distância, sem o usuário ter que ler.

### Tom

Sóbrio e direto. É dinheiro do usuário, não uma rede social: nada de gamificação, confete ou
linguagem publicitária. Deve dar para bater o olho e entender a situação do mês em dois
segundos. Números são o conteúdo principal — tipografia e alinhamento importam mais que
ilustração.

Valores em real brasileiro, no formato `R$ 1.234,56`. Datas em `dd/mm/aaaa`. Meses como
"setembro de 2026".

---

## Estrutura: 11 telas

### Públicas (3) — sem estar logado

**1. Home (`/`)**
Página de entrada do site. É a primeira coisa que alguém vê ao chegar.
Precisa explicar o que o app faz e levar a pessoa a criar conta.
- Explicação curta do produto, em linguagem de gente
- Demonstração visual do que o app entrega (uma prévia do painel, por exemplo)
- Chamada principal: criar conta. Secundária: entrar
- Quem já está logado deve ver "Ir para o app" no lugar de "Entrar"
- É a única tela com cara de site; as outras 10 têm cara de aplicação

**2. Entrar (`/login`)**
E-mail e senha, mais um botão "Entrar com Google". Link para criar conta e um estado de erro
para credencial inválida — a mensagem é sempre a mesma, genérica, sem dizer se o e-mail
existe. Precisa também de um estado para "tentativas demais, tente de novo em X minutos".

**3. Criar conta (`/registro`)**
Nome, e-mail e senha (mínimo 8 caracteres), mais o botão do Google. Ao criar, a pessoa já
entra direto no app — não há tela de confirmação nem verificação de e-mail.

### O app (6) — depois de entrar

**4. Painel (`/dashboard`)** — a tela inicial de quem está logado
O resumo do mês selecionado. É a tela que responde "como eu estou?".
- Quanto entrou, quanto saiu, saldo do mês
- Quanto está a pagar e quanto está atrasado, visualmente separados do saldo
- Quantidade de lançamentos em cada situação
- Gasto por categoria, do maior para o menor, com o percentual de cada uma
- Atalhos para as ações mais comuns: nova despesa, nova receita

**5. Despesas (`/despesas`)** — a tela mais usada do app
Lista das despesas do mês, com a situação de cada uma bem visível.
- Cada linha mostra: descrição, categoria, valor, vencimento e se está paga
- Marcar como paga (e desmarcar) direto na linha, sem sair da lista
- Filtrar por período e por situação (todas / pagas / a pagar)
- Despesas atrasadas precisam se destacar das demais
- Uma despesa gerada por uma recorrência tem indicação disso
- Criar e editar acontecem em **modal**, não em página separada

**6. Receitas (`/receitas`)**
Mais simples que despesas: receita não tem categoria nem estado de pagamento — ou foi
recebida, ou não existe.
- Cada linha: descrição, valor, data de recebimento
- Mesmo padrão de lista, filtro por período e modal de criação

**7. Orçamento (`/orcamento`)**
Onde o usuário define quanto pretende gastar por categoria e vê como está indo.
É a tela com o desafio visual mais interessante do app, porque cada categoria tem
**dois números e duas leituras**:
- O limite definido
- Quanto já foi **gasto** (percentual do limite)
- Quanto ainda está **a pagar** (que vai consumir o limite quando for quitado)
- Um aviso quando já estourou, e um aviso diferente para "vai estourar se pagar tudo"

O segundo aviso é o que tem valor: avisa antes, não depois. Precisa ser visualmente
distinto de "já estourou".

Categorias **sem limite definido** também aparecem na lista, mostrando o gasto mas sem
percentual nem barra — não há limite contra o que comparar. Definir um limite acontece ali
mesmo, na própria linha.

Ordem: as com limite primeiro, da mais apertada à mais folgada; depois as sem limite, da que
mais gastou à que menos.

**8. Metas (`/metas`)**
Objetivos de poupança, como "reserva de emergência" ou "viagem".
- Cada meta mostra: título, quanto já foi guardado, quanto falta para o alvo, progresso
  visual e a data alvo quando houver
- Meta atingida tem tratamento visual próprio
- Duas ações por meta: guardar dinheiro (aporte) e retirar (resgate), ambas em modal

**9. Recorrentes (`/recorrentes`)**
Contas que se repetem todo mês — aluguel, assinatura, mensalidade. Aqui o usuário cadastra
o **modelo**, e a partir dele o app gera as despesas de cada mês.
- Cada modelo mostra: descrição, categoria, valor, dia do vencimento, mês de início e de fim
  (quando houver), e se está ativo
- Ação de gerar as ocorrências até um mês escolhido, com retorno de quantas foram criadas
- A relação entre "modelo" e "despesas geradas" precisa ficar clara — é o conceito menos
  óbvio do app e o que mais confunde

### Configuração (2)

**10. Categorias (`/categorias`)**
Categorias são usadas só em despesas, nunca em receitas.
- Criar, renomear, arquivar e reativar
- Categoria arquivada não aceita lançamentos novos, mas continua aparecendo no histórico —
  precisa de tratamento visual distinto de "excluída"
- Excluir só é possível se nenhuma despesa usar a categoria; quando não for possível, o
  motivo precisa ficar claro

**11. Conta (`/conta`)**
- Dados do usuário: nome, e-mail, data de cadastro, se entra pelo Google
- Trocar a senha, pedindo a senha atual
- Sair do app
- Quem entra só pelo Google não tem senha para trocar, e essa tela precisa dizer isso em vez
  de mostrar um formulário que não funciona

---

## O que **não** é tela

Estas são as ações que acontecem em **modal ou diálogo**, dentro da tela onde a pessoa já
está. Nenhuma delas deve tirar o usuário da lista em que estava:

nova/editar despesa · nova/editar receita · nova/editar meta · aporte · resgate ·
nova/editar recorrente · definir limite de orçamento · confirmar exclusão

O critério: rota é lugar, modal é ação. Você navega para "minhas despesas"; você não navega
para "estou digitando uma despesa".

---

## Elementos que atravessam várias telas

**Seletor de mês.** Painel, despesas e orçamento são sempre "de um mês". O seletor precisa
ser o mesmo componente nas três, ficar sempre no mesmo lugar, e manter o mês escolhido ao
trocar de tela. É o controle mais usado do app depois dos botões de criar.

**Navegação.** Menu lateral no desktop com as 6 telas do app; Categorias e Conta agrupados
como configuração, mais discretos. No celular, navegação inferior com as 3 ou 4 telas
principais e o resto num menu.

**Estados vazios.** Uma conta nova chega com 8 categorias criadas e **nada mais**: nenhuma
despesa, nenhuma receita, nenhuma meta, nenhum orçamento. Então a primeira experiência do app
é, quase inteira, de telas vazias. Cada estado vazio precisa explicar o que aquela tela faz e
oferecer a ação para começar — não é caso de exceção, é a primeira impressão.

**Estados de carregamento e erro.** Toda lista precisa de um estado enquanto carrega e de um
estado para falha, com opção de tentar de novo.

**Valores negativos.** O saldo do mês pode ser negativo, e o quanto resta de um orçamento
estourado também. Precisam de tratamento visual claro, sem depender só de cor — daltonismo é
comum, e isto é um app de números.

**Responsivo.** Precisa funcionar bem no celular: registrar uma despesa é algo que se faz na
fila do mercado, não sentado no computador.

**Tema claro e escuro.**

---

## O que **não** incluir

- Recuperação de senha por e-mail ou verificação de e-mail — não existem no produto
- Relatórios avançados, gráficos de evolução entre meses, exportação, previsão
- Múltiplas contas bancárias, cartões, faturas, parcelamento
- Qualquer coisa de time: convites, compartilhamento, permissões, comentários
- Notificações e lembretes
- Múltiplas moedas na interface (o sistema suporta, mas o design pode assumir real)

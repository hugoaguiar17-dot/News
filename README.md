# Notícias PT

App Android com notícias de última hora de fontes **gratuitas**, portuguesas e internacionais (inglês), com leitura do artigo completo em modo de leitura.

## Funcionalidades
- Notícias de todas as fontes juntas, ordenadas pela mais recente; etiqueta **ÚLTIMA HORA** na primeira hora
- Separadores Tudo / Portugal / Desporto / Mundo e filtro por fonte
- Atualiza ao abrir e de 5 em 5 minutos; puxar para baixo para atualizar
- Leitor do artigo completo (texto, imagens, citações), com tamanho de letra ajustável
- Se o texto não for extraído, abre a página original dentro da app
- Ecrã **Fontes**: ligar/desligar fontes e adicionar qualquer feed RSS
- **Notificações**: toca no sino de cada fonte para seres avisado das notícias novas (verificação ~15 em 15 min; tocar na notificação abre o artigo)

## Fontes incluídas
**Portugal:** RTP Notícias, Notícias ao Minuto, SIC Notícias, CNN Portugal, TSF, Euronews PT
**Desporto:** Bola na Rede, Maisfutebol, zerozero, O Jogo (O Jogo tem alguns artigos Premium; esses abrem só em parte)
**Mundo (inglês):** BBC News, The Guardian

Se alguma fonte mudar o endereço do feed, aparece "Sem resposta de: …" no topo. Desliga-a ou adiciona o endereço novo em Fontes.

## Instalar
Cada envio para o branch `main` compila um APK novo automaticamente (GitHub Actions).
1. No telemóvel, abre o repositório → **Releases** → descarrega o `NoticiasPT-v1.0.X.apk` mais recente
2. Abre o ficheiro e permite "instalar de fontes desconhecidas" quando o Android pedir
3. As versões seguintes instalam por cima, sem perder as definições

## Nota
O APK é assinado com a chave em `keystore/`. Mantém o repositório **privado**.

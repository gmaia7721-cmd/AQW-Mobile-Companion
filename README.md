# AQW Mobile Companion

Companion visual para Android e Artix Experimental Launcher.

## Versão 0.2
- Painel flutuante Android.
- Scanner de tela com autorização explícita do Android (MediaProjection).
- Reconhecimento de texto offline com Google ML Kit Text Recognition.
- Exibe os textos encontrados para ajudar a localizar nomes de monstros.
- Não lê HP, não automatiza combate, não lê memória, não injeta código nem altera o cliente AQW.

## Limitações do scanner
O OCR reconhece texto visível na imagem; ele não conhece sozinho o catálogo de monstros do AQW e pode confundir nomes de NPCs, jogadores, chat ou elementos da interface. A identificação exata de cada criatura em todos os mapas exigirá uma etapa adicional de validação/classificação. A captura depende da autorização de compartilhamento de tela do Android.

## Compilar
O GitHub Actions executa `assembleDebug` e publica o APK como artefato do workflow.

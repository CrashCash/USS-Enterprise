# App for the Playmobil USS Enterprise

Playmobil broke the Android app to control this, so I wrote a replacement.

This communicates via Bluetooth Low Energy.

I reverse engineered the protocol via the phone's developer-mode Bluetooth
snoop logs. It's very simple.

The original app uses Unity for all the player activities, and I just
implement the command mode using just the stock Android UI.

Everything works, except the volume control...

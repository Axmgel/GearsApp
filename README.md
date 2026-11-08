# GearsApp

App de Android (Kotlin) para calcular engranes rectos, helicoidales y cónicos, y su fabricación en
cabezal divisor, fresadora universal y generadora de engranajes. Funciona sin internet.

## Estructura

- `core/`: todos los cálculos en Kotlin puro, sin Android. Aquí van las fórmulas y sus pruebas.
- `app/`: la aplicación Android (pantallas con Jetpack Compose y guardado de las máquinas del taller).

## Cómo abrirlo

1. Abre la carpeta del proyecto en Android Studio.
2. Espera a que termine de sincronizar Gradle.
3. Ejecuta la configuración `app` en un teléfono o emulador.

Para correr sólo las pruebas de los cálculos (no necesita el SDK de Android):

```
./gradlew -PsoloCore=true :core:test
```

## Plan

1. Base de la app y "Configurar máquinas".
2. Cálculo de maquinaria: generadora (sincronización y tren de hélice) y cabezal divisor
   (encontrador de puntos y sincronización helicoidal).
3. Cálculo de engranes: rectos, helicoidales, cónicos y cónicos helicoidales, con explicación paso a paso.
4. Imprimir el cálculo a PDF y preparar la publicación en Play Store.

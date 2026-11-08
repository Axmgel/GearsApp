# Cómo publicar GearsApp en Play Store

## 1. Crear tu llave de firma (una sola vez)

Google pide que cada versión de la app vaya firmada con la misma llave. **Guarda la llave y sus
contraseñas en un lugar seguro: si la pierdes no podrás actualizar la app.**

En Android Studio: *Build → Generate Signed App Bundle or APK → Android App Bundle → Create new…*
y guarda el archivo, por ejemplo, como `gearsapp.jks` en la carpeta del proyecto.

## 2. Decirle al proyecto dónde está la llave

Crea un archivo `keystore.properties` en la carpeta principal del proyecto (junto a `settings.gradle.kts`):

```
storeFile=gearsapp.jks
storePassword=TU_CONTRASEÑA
keyAlias=TU_ALIAS
keyPassword=TU_CONTRASEÑA_DE_LA_LLAVE
```

Este archivo y la llave (`.jks`) **no se suben a GitHub**; ya están en `.gitignore`.

## 3. Generar el paquete para Play Store

```
./gradlew :app:bundleRelease
```

El archivo queda en `app/build/outputs/bundle/release/app-release.aab`.

## 4. Subirlo a Play Console

1. Crea una cuenta de desarrollador en https://play.google.com/console (pago único).
2. *Crear app* → nombre "GearsApp", idioma español, gratis o de pago.
3. Llena la ficha: descripción, capturas de pantalla (al menos 2 del teléfono) e ícono de 512×512.
4. *Política de privacidad*: la app no pide permisos ni junta datos. Puedes publicar el texto de
   `POLITICA_DE_PRIVACIDAD.md` (por ejemplo en GitHub Pages) y poner ese enlace.
5. *Contenido de la app*: sin anuncios, sin datos personales, para público general.
6. *Pruebas*: las cuentas nuevas deben hacer primero una prueba cerrada con testers antes de
   publicar a producción; Play Console te dice cuántos y cuántos días.
7. Sube el `.aab` en *Producción → Crear versión* (o en la prueba cerrada).

## 5. Versiones siguientes

Antes de cada nueva versión sube `versionCode` (1, 2, 3…) y `versionName` en `app/build.gradle.kts`.

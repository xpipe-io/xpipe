# Bóveda Git XPipe

XPipe puede sincronizar todos tus datos de conexión con tu propio repositorio remoto git. Puedes sincronizar con este repositorio en todas las instancias de la aplicación XPipe de la misma manera, cada cambio que hagas en una instancia se reflejará en el repositorio.

En primer lugar, necesitas crear un repositorio remoto con el proveedor git que prefieras. Este repositorio tiene que ser privado.
A continuación, sólo tienes que copiar y pegar la URL en la configuración del repositorio remoto de XPipe.

También necesitas tener un cliente `git` instalado localmente en tu máquina local. Puedes probar a ejecutar `git` en un terminal local para comprobarlo.
Si no tienes uno, puedes visitar [https://git-scm.com](https://git-scm.com/) para instalar git.

## Autenticarse en el repositorio remoto

Hay varias formas de autenticarse. La mayoría de los repositorios utilizan HTTPS, donde tienes que especificar un nombre de usuario y una contraseña.
Algunos proveedores también admiten el protocolo SSH, que también es compatible con XPipe.
Si utilizas SSH para git, probablemente sepas cómo configurarlo, así que esta sección cubrirá sólo HTTPS.

Necesitas configurar tu CLI de git para poder autenticarte con tu repositorio git remoto a través de HTTPS. Hay varias formas de hacerlo.
Puedes comprobar si ya está hecho reiniciando XPipe una vez configurado un repositorio remoto.
Si te pide tus credenciales de acceso, necesitas configurarlas.

Muchas herramientas especiales como esta [GitHub CLI](https://cli.github.com/) lo hacen todo automáticamente por ti cuando se instalan.
Algunas versiones más recientes del cliente git también pueden autenticarse a través de servicios web especiales en los que sólo tienes que acceder a tu cuenta en el navegador.

También hay formas manuales de autenticarse mediante un nombre de usuario y un token.
Hoy en día, la mayoría de los proveedores requieren un token de acceso personal (PAT) para autenticarse desde la línea de comandos en lugar de las contraseñas tradicionales.
Puedes encontrar páginas comunes (PAT) aquí:
- **GitHub**: [Tokens de acceso personal (clásico)](https://github.com/settings/tokens)
- **GitLab**: [Ficha de acceso personal](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Clave de acceso personal](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Configuración -> Aplicaciones -> Sección Gestionar tokens de acceso`
Establece el permiso del token para el repositorio en Lectura y Escritura. El resto de permisos del token pueden establecerse como Lectura.
Aunque tu cliente git te pida una contraseña, debes introducir tu token a menos que tu proveedor aún utilice contraseñas.
- La mayoría de los proveedores ya no admiten contraseñas.

Si no quieres introducir tus credenciales cada vez, puedes utilizar cualquier gestor de credenciales git para ello.
Para más información, consulta por ejemplo
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Algunos clientes git modernos también se encargan de almacenar las credenciales automáticamente.

Si todo va bien, XPipe debería enviar un commit a tu repositorio remoto.

## Añadir categorías al repositorio

Por defecto, no se establecen categorías de conexión para sincronizar, de modo que tengas un control explícito sobre qué conexiones confirmar.
Así que al principio, tu repositorio remoto estará vacío.

Para que tus conexiones de una categoría se pongan dentro de tu repositorio git,
tienes que hacer clic en el icono del engranaje (al pasar el ratón por encima de la categoría)
en la pestaña `Conexiones` de la vista general de la categoría, a la izquierda.
Luego haz clic en `Añadir al repositorio git` para sincronizar la categoría y las conexiones con tu repositorio git.
Esto añadirá todas las conexiones sincronizables al repositorio git.

## Las conexiones locales no se sincronizan

Cualquier conexión localizada en la máquina local no se puede compartir, ya que se refiere a conexiones y datos que sólo están disponibles en el sistema local.

Algunas conexiones que se basan en un archivo local, por ejemplo las configuraciones SSH, pueden compartirse a través de git si los datos subyacentes, en este caso el archivo, se han añadido también al repositorio git.

## Añadir archivos a git

Cuando todo esté configurado, tienes la opción de añadir también a git cualquier archivo adicional, como claves SSH.
Junto a cada archivo elegido hay un botón git que añadirá el archivo al repositorio git.
Estos archivos también se encriptan cuando se envían.

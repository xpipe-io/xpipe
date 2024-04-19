# Intégration de bureau RDP

Tu peux utiliser cette connexion RDP dans XPipe pour lancer rapidement des applications et des scripts. Cependant, en raison de la nature du RDP, tu dois modifier la liste d'autorisation des applications distantes sur ton serveur pour que cela fonctionne. De plus, cette option permet le partage de lecteur pour exécuter tes scripts sur ton serveur distant.

Tu peux aussi choisir de ne pas le faire et d'utiliser simplement XPipe pour lancer ton client RDP sans utiliser de fonctions d'intégration de bureau avancées.

## RDP allow lists

Un serveur RDP utilise le concept des listes d'autorisation pour gérer le lancement des applications. Cela signifie essentiellement qu'à moins que la liste d'autorisation ne soit désactivée ou que des applications spécifiques n'aient été explicitement ajoutées à la liste d'autorisation, le lancement direct d'applications distantes échouera.

Tu peux trouver les paramètres de la liste d'autorisation dans le registre de ton serveur à `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Autoriser toutes les applications

Tu peux désactiver la liste d'autorisation pour permettre à toutes les applications distantes d'être lancées directement à partir de XPipe. Pour cela, tu peux exécuter la commande suivante sur ton serveur en PowerShell : `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAllowList' -Name "fDisabledAllowList" -Value 1`.

### Ajout d'applications autorisées

Tu peux aussi ajouter des applications distantes individuelles à la liste. Cela te permettra alors de lancer les applications listées directement à partir de XPipe.

Sous la clé `Applications` de `TSAppAllowList`, crée une nouvelle clé avec un nom arbitraire. La seule exigence pour le nom est qu'il soit unique parmi les enfants de la clé "Applications". Cette nouvelle clé doit contenir les valeurs suivantes : `Name`, `Path` et `CommandLineSetting`. Tu peux effectuer cette opération dans PowerShell à l'aide des commandes suivantes :

```
$appName="Notepad"
$appPath="C:\NWindows\NSystem32\NNotepad.exe"

$regKey="HKLM:\NSOFTWARE\NMicrosoft\NWindows NT\NCurrentVersion\NTerminal Server\NTSAllowList\NApplications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\NappName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Si tu veux autoriser XPipe à exécuter des scripts et à ouvrir des sessions de terminal, tu dois également ajouter `C:\NWindows\NSystem32\cmd.exe` à la liste des autorisations.

## Considérations de sécurité

Cela ne rend en aucun cas ton serveur non sécurisé, car tu peux toujours exécuter les mêmes applications manuellement lors du lancement d'une connexion RDP. Les listes d'autorisation ont plutôt pour but d'empêcher les clients d'exécuter instantanément n'importe quelle application sans l'intervention de l'utilisateur. En fin de compte, c'est à toi de décider si tu fais confiance à XPipe pour cela. Tu peux lancer cette connexion sans problème, cela n'est utile que si tu veux utiliser l'une des fonctions d'intégration de bureau avancées de XPipe.

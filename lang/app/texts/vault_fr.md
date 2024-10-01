# XPipe Git Vault

XPipe peut synchroniser toutes tes données de connexion avec ton propre dépôt distant git. Tu peux te synchroniser avec ce dépôt dans toutes les instances de l'application XPipe de la même manière, chaque changement que tu fais dans une instance sera reflété dans le dépôt.

Tout d'abord, tu dois créer un dépôt distant avec le fournisseur git de ton choix. Ce dépôt doit être privé.
Il te suffit ensuite de copier et de coller l'URL dans les paramètres du dépôt à distance de XPipe.

Tu dois également avoir un client `git` installé localement et prêt sur ta machine locale. Tu peux essayer d'exécuter `git` dans un terminal local pour vérifier.
Si tu n'en as pas, tu peux visiter [https://git-scm.com](https://git-scm.com/) pour installer git.

## S'authentifier auprès du dépôt distant

Il y a plusieurs façons de s'authentifier. La plupart des dépôts utilisent le protocole HTTPS pour lequel tu dois spécifier un nom d'utilisateur et un mot de passe.
Certains fournisseurs prennent également en charge le protocole SSH, qui est également pris en charge par XPipe.
Si tu utilises SSH pour git, tu sais probablement comment le configurer, c'est pourquoi cette section ne traitera que du HTTPS.

Tu dois configurer ton CLI git pour pouvoir t'authentifier auprès de ton dépôt git distant via HTTPS. Il y a plusieurs façons de le faire.
Tu peux vérifier si c'est déjà fait en redémarrant XPipe une fois qu'un dépôt distant est configuré.
S'il te demande tes identifiants de connexion, tu dois les configurer.

De nombreux outils spéciaux comme ce [GitHub CLI] (https://cli.github.com/) font tout automatiquement pour toi lorsqu'ils sont installés.
Certaines versions plus récentes du client git peuvent également s'authentifier via des services web spéciaux où il te suffit de te connecter à ton compte dans ton navigateur.

Il existe également des moyens manuels de s'authentifier via un nom d'utilisateur et un jeton.
De nos jours, la plupart des fournisseurs exigent un jeton d'accès personnel (PAT) pour s'authentifier à partir de la ligne de commande au lieu des mots de passe traditionnels.
Tu peux trouver des pages communes (PAT) ici :
- **GitHub** : [Jetons d'accès personnels (classiques)](https://github.com/settings/tokens)
- **GitLab** : [Jeton d'accès personnel](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket** : [Jeton d'accès personnel](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea** : `Paramètres -> Applications -> Section Gérer les jetons d'accès`
Définis la permission du jeton pour le référentiel sur Lecture et Écriture. Le reste des permissions du jeton peut être défini comme Lecture.
Même si ton client git te demande un mot de passe, tu dois saisir ton jeton, sauf si ton fournisseur utilise encore des mots de passe.
- La plupart des fournisseurs ne prennent plus en charge les mots de passe.

Si tu ne veux pas entrer tes informations d'identification à chaque fois, tu peux utiliser n'importe quel gestionnaire d'informations d'identification git pour cela.
Pour plus d'informations, voir par exemple :
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Certains clients git modernes se chargent également de stocker les informations d'identification automatiquement.

Si tout se passe bien, XPipe devrait pousser un commit vers ton dépôt distant.

## Ajouter des catégories au dépôt

Par défaut, aucune catégorie de connexion n'est définie pour la synchronisation afin que tu aies un contrôle explicite sur les connexions à valider.
Ainsi, au début, ton dépôt distant sera vide.

Pour que les connexions d'une catégorie soient placées à l'intérieur de ton dépôt git, tu dois cliquer sur l'icône de l'engrenage,
tu dois cliquer sur l'icône de l'engrenage (lorsque tu survoles la catégorie)
dans ton onglet `Connexions` sous l'aperçu de la catégorie sur le côté gauche.
Cliquez ensuite sur `Ajouter au dépôt git` pour synchroniser la catégorie et les connexions avec votre dépôt git.
Cela ajoutera toutes les connexions synchronisables au dépôt git.

## Les connexions locales ne sont pas synchronisées

Toute connexion située sous la machine locale ne peut pas être partagée car elle fait référence à des connexions et des données qui ne sont disponibles que sur le système local.

Certaines connexions basées sur un fichier local, par exemple les configurations SSH, peuvent être partagées via git si les données sous-jacentes, dans ce cas le fichier, ont également été ajoutées au dépôt git.

## Ajouter des fichiers à git

Lorsque tout est configuré, tu as la possibilité d'ajouter à git des fichiers supplémentaires tels que des clés SSH.
À côté de chaque choix de fichier se trouve un bouton git qui ajoutera le fichier au dépôt git.
Ces fichiers sont également cryptés lorsqu'ils sont poussés.

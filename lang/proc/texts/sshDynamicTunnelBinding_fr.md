## Liaison par tunnel

Les informations de liaison que tu fournis sont transmises directement au client `ssh` de la manière suivante : `-D [adresse :]port`.

Par défaut, l'adresse sera liée à l'interface loopback. Tu peux également utiliser des caractères génériques, par exemple en définissant l'adresse sur `0.0.0.0` afin de lier toutes les interfaces réseau accessibles via IPv4. Lorsque tu omets complètement l'adresse, le caractère générique `*`, qui autorise les connexions sur toutes les interfaces réseau, sera utilisé. Note que certaines notations d'interfaces réseau peuvent ne pas être prises en charge par tous les systèmes d'exploitation. Les serveurs Windows, par exemple, ne prennent pas en charge le caractère générique `*`.

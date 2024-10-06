### Utilisation


commande pour lancer le programme : 

`java -jar "./out/artifacts/RegEx_jar/RegEx.jar" "Regex_ici" "path_to_file" "debug=true/false" `


exemple : 

`java -jar "./out/artifacts/RegEx_jar/RegEx.jar" "Regex_ici" "./testbed/56667-0.txt" "false" `


commande pour lancer les tests :

`java -jar .\out\artifacts\RegExTests\RegExTests.jar`


Commande pour lancer les tests de performance utilisant hyperfine :


`.\hyperfine\hyperfine --runs (nombre de tests ex = 5)  'commande 1'  'commande 2' '...commande 3 etc'  --export-json nom_export.json`

exemple : 

`.\hyperfine\hyperfine --runs 1000 'egrep "Sargon" "Babylon.txt"'  "Java -jar .\out\artifacts\RegEx_jar\RegEx.jar 'Sargon' '.\testbed\56667-0.txt' 'false'"  --export-json sargon.json`

lance 1000 tests de chaque commande et exporte les resultats dans sargon.json
et ensuite on peut lancé le script python pour afficher les resultats :

`python ./plot_histogram.py sargon.json`
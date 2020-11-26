
Put the jar file of your agent in ./agents

Create a tournament:

java -jar './logist/logist.jar' -new 'tour' './agents'

Run the tournament:

java -jar './logist/logist.jar' -run 'tour' './config/auction.xml'

Save the results:

java -jar './logist/logist.jar' -score 'tour'



Note also that the scoring of a tournament does not work if the name of your .jar file contains dashes "-".

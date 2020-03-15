# Stop Epidemic

Stop Epidemic is a mobile app that privately tracks exposure to a virus and lets the user know when, and how much, he/she has been exposed.

## How it works?

Every modern phone contains a Bluetooth Low Energy (BLE) chip. The BLE chip:
1. Can create a beacon that periodically transmits a signal containing a unique ID and a bit of data. The transmitted signal is short in range (theoretically up to 100m).
2. Can listen to beacons with a given ID in the vicinity (up to 100m) and list to the data that has been sent.
3. Uses negligible amounts of energy for operations 1 and 2.

This app does the following: 
1. It creates a beacon with a preset ID ('0000DEAD-B644-4520-8F0C-720EAF059935') and periodically transmits a random number that changes every day. The randomly generated number is between 0 to 4294967296 (32 bits in width). Every other phone can listen to this beacon and can get this random number if they are in the vicinity. 
2. It listens to all beacons with the preset ID and receives the random numbers transmitted by the other phone. A combination of the random number from the user's app and the received random number uniquely identifies an *encounter*. One encounter is uniquely represented by a number between 0 and 2^64 and is the same for the transmitting phone and the receiving phone (the same combination of numbers). The app memorizes all the encounters that happened in the relevant period (e.g., past 3 months). 
3. When a person gets infected they submit all their past encounters (list of numbers between 0 and 2^64) to a central publicly-available database of *infected encounters*. Since all the numbers are random and they are changing frequently, no private information is leaked.
4. Everyone else can check in the public database if their encounters (stored on the phone) have been infected. If yes, they can follow instructions for their exposure level. Again, no private information is leaked as all the encounters (random number pairs) are stored on the phone. 

## What do we need for the app to succeed?

0. The app needs to be tested with a large number (approximately 100) of devices that are both Android and iOS. We need to be sure that BLE can account for all encounters properly. 

1. Apple and Google need to allow this app to transmit the information in the background. This is possible but currently prohibited by iOS and Android. A simple phone update would make this possible.

2. *Everyone* needs to install the app and act according to recommendations.

## What do we get?

1. One will know when, and how much, they have been exposed to COVID-19 or any future deadly virus.

2. One will know if you are likely to be a patient 0 helping epidemiologists contain the spread of disease.

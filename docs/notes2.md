# Notes of Decisions or Doubts

## Could be necessary to make the real time update of statistics of barrels and downloaders to make the web interactive. How to make it?
- When the client first requests the real time statistics, it will subscribe himself automatically to receive new admin console updates, and gateway will reply the current statistics of the system, and then when new statistics are available, the gateway will print on the client the updated statistics via callback, if the client wants no more updates, he can press any key to get out of the administration console and automatically unsubscribe.
- The function which the gateway uses for writing on the client first verifies if the client is still subscribed before printing the message, so it does not write over other prints in case the client already exited the administration console.

### Consider to use one thread per User for the Gateway to print on the Users console?
- It would be a good idea because if is only one thread or process, the other clients would have to wait to the client before him in the list to receive the message, which would be bad because for example, the network to the User could be congested.

### But how to control the variable "statisticsUpdateCount"?
- The threads share the variable and the variable was supposed to track the changes of information.
- Maybe use an hash table that holds the "statisticsUpdateCount" per Client.
- A variable may not be such a good idea since the system would be in busy waiting, so consider using an hash table with a semaphore per user.

### But how to update the semaphores count for new statistics?
- If each statistic changer, like for example a new barrel, updates the semaphores there would be a slowdown. So consider using a main semaphore that each stats update will increase and that main semaphore will be controlled by the main thread of the gateway that after its unlock will update all the others semaphores.

## Notes:

- The barrels are exchanging between each other of the data received from the downloader, but are not currently exchanging stats info. #TODO

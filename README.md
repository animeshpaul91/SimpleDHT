# Simple Distributed Hash Table
## A simplified implementation of a Distributed Hash Table based on the Chord Architecture

This project is aimed at the implementation of a simplified Distributed Hash Table using Consistent Hashing based on the chord architecture. In an android environment, there are five emulators which were used to store data. The following functionalities were implemented in the project: 
1. ID space Partitioning/Re-Partitioning
2. Ring-based routing
3. Node joins

There were basic CRUD operations that were implemented. These are the basic operations: 
1. Create (Insert Data)
2. Read which returns the most recent write (Query Data)
3. Update (Overwrite Existing Data)
4. Delete (Delete Data)

## Brief Description of Project

### ID space Partitioning
Nodes (emulators) are laid along a circular ring based on their SHA-1 hash value. 

### Replication
Whenever a insert operation occurs, the MD5 hash of the key is obtained to correctly identify the node that is responsible for storing the data. The data is then stored on this node (the coordinator). The coordinator then routes the data to its next two successors. In this way, copies of data are redundantly stored all over the ring. Whenever an existing data is updated, the data is versioned (stored in multiple versions) across the node. This allows the latest versioned-value to be returned when a query occurs later. 

### Node Joins
Whenever a node joins it takes up responsibility of its share of the data. It gets all copies of the data from its immediate successor and its previous 2 predecessors. Throughout the project, it is assumed that every node is aware of its neighbors (successor and two predecessors).Now this node can participate in client requests (insert, read, update, delete).

References: 
1. [Chord Paper] (https://cse.buffalo.edu/~stevko/courses/cse486/spring19/files/chord_sigcomm.pdf)
2. [Project Description] (https://cse.buffalo.edu/~eblanton/course/cse586-2018-0s/materials/2018S/simple-dht.pdf)


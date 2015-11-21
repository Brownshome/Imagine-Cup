CREATE DATABASE IF NOT EXISTS sqlbackend;

USE sqlbackend;

CREATE TABLE IF NOT EXISTS LoginData (
	Username VARCHAR(255) NOT NULL PRIMARY KEY,
	Hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS UserPrefs (
	Username VARCHAR(255) NOT NULL,
	NonFriendsJoin BOOLEAN DEFAULT false,
	FriendsJoin BOOLEAN DEFAULT false,
	NonFriendsInvite BOOLEAN DEFAULT false,
	FriendsInvite BOOLEAN DEFAULT false,
	ShareInfo BOOLEAN DEFAULT false,
	
	PRIMARY KEY (Username),
	FOREIGN KEY (Username) REFERENCES LoginData(Username)
);

CREATE TABLE IF NOT EXISTS FriendRequests (
	UserFrom VARCHAR(255) NOT NULL,
	UserTo VARCHAR(255) NOT NULL,
	Message VARCHAR(255),
	DateMade TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	Accepted BOOLEAN DEFAULT FALSE,
	DateAccepted TIMESTAMP,
	
	FOREIGN KEY (UserFrom) REFERENCES LoginData(Username),
	FOREIGN KEY (UserTo) REFERENCES LoginData(Username)
);

CREATE TABLE IF NOT EXISTS UserData (
	Username VARCHAR(255) NOT NULL,
	DateJoined TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	LastSeen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	AvatarData VARBINARY(64) DEFAULT X'',
	Status VARCHAR(255) DEFAULT '',
	
	PRIMARY KEY (Username),
	FOREIGN KEY (Username) REFERENCES LoginData(Username)
);

CREATE TABLE IF NOT EXISTS ChatHistory (
	UserFrom VARCHAR(255) NOT NULL,
	UserTo VARCHAR(255) NOT NULL,
	Text VARCHAR(512),
	DateMade TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	
	FOREIGN KEY (UserFrom) REFERENCES LoginData(Username),
	FOREIGN KEY (UserTo) REFERENCES LoginData(Username)
);

CREATE TABLE IF NOT EXISTS ArenaHistory (
	Username VARCHAR(255) NOT NULL,
	Event VARCHAR(64) CHECK (Event IN ('CHAT_FROM', 'CHAT_TO', 'REQUEST_TO_MADE', 'REQUEST_FROM_MADE', 'REQUEST_TO_ACCEPTED', 'REQUEST_FROM_ACCEPTED', 'JOIN', 'LEAVE', 'CREATE', 'CLOSE', 'FILE_SEND')),
	Data VARCHAR(255),
	DateMade TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	
	FOREIGN KEY (Username) REFERENCES LoginData(Username)
);
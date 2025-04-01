/* (drop if exists) create and set new schema */
DROP SCHEMA IF EXISTS `cryptoac`;
CREATE SCHEMA `cryptoac`;
USE `cryptoac`;

/* to enable triggers in the database */
SET GLOBAL log_bin_trust_function_creators = 1;

/* remember that MySQL automatically creates indexes for primary keys and unique fields */

/* re-create tables */
CREATE TABLE `users` (
  `username` varchar(100),
  `userToken` char(20) NOT NULL UNIQUE,
  `userVersionNumber` int NOT NULL,
  `asymEncPublicKey` varchar(500) NOT NULL,
  `asymSigPublicKey` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL,
  `isAdmin` boolean NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `deletedUsers` (
  `username` varchar(100),
  `userToken` char(20) NOT NULL UNIQUE,
  `userVersionNumber` int NOT NULL,
  `asymEncPublicKey` varchar(500) NOT NULL,
  `asymSigPublicKey` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL,
  `isAdmin` boolean NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `roles` (
  `roleName` varchar(100),
  `roleToken` char(20) NOT NULL UNIQUE,
  `asymEncPublicKey` varchar(500) NOT NULL,
  `asymSigPublicKey` varchar(500) NOT NULL,
  `roleVersionNumber` int NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `deletedRoles` (
  `roleName` varchar(100),
  `roleToken` char(20) NOT NULL UNIQUE,
  `asymEncPublicKey` varchar(500) NOT NULL,
  `asymSigPublicKey` varchar(500) NOT NULL,
  `roleVersionNumber` int NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `resources` (
  `resourceName` varchar(100),
  `resourceToken` char(20) NOT NULL UNIQUE,
  `resourceVersionNumber` int,
  `reEncryptionThresholdNumber` int NOT NULL,
  `status` varchar(20) NOT NULL,
  `enforcement` varchar(50) NOT NULL,
  `encryptedSymKey` varchar(3000),
  PRIMARY KEY (`resourceName`, `resourceVersionNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `deletedResources` (
  `resourceName` varchar(100),
  `resourceToken` char(20) NOT NULL UNIQUE,
  `resourceVersionNumber` int,
  `reEncryptionThresholdNumber` int NOT NULL,
  `status` varchar(20) NOT NULL,
  `enforcement` varchar(50) NOT NULL,
  `encryptedSymKey` varchar(3000),
  PRIMARY KEY (`resourceName`, `resourceVersionNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `usersRoles` (
  `username` varchar(100),
  `roleName` varchar(100),
  `encryptedAsymEncPublicKey` varchar(3000) NOT NULL,
  `encryptedAsymEncPrivateKey` varchar(3000) NOT NULL,
  `encryptedAsymSigPublicKey` varchar(3000) NOT NULL,
  `encryptedAsymSigPrivateKey` varchar(3000) NOT NULL,
  `userVersionNumber` int NOT NULL,
  `roleVersionNumber` int NOT NULL,
  `signature` text(344) NOT NULL,
  PRIMARY KEY (`username`, `roleName`),
  KEY `fk_usersRoles_username_idx` (`username`),
  KEY `fk_usersRoles_roleName_idx` (`roleName`),
  CONSTRAINT `fk_usersRoles_username` FOREIGN KEY (`username`) REFERENCES `users` (`username`),
  CONSTRAINT `fk_usersRoles_roleName` FOREIGN KEY (`roleName`) REFERENCES `roles` (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `rolesPermissions` (
  `roleName` varchar(100),
  `resourceName` varchar(100),
  `roleToken` char(20),
  `resourceToken` char(20),
  `encryptedSymKey` varchar(3000) NOT NULL,
  `roleVersionNumber` int NOT NULL,
  `resourceVersionNumber` int NOT NULL,
  `operation` varchar(50) NOT NULL,
  `signerToken` char(20) NOT NULL,
  `signature` text(344) NOT NULL,
  PRIMARY KEY (`roleName`, `resourceName`, `resourceVersionNumber`),
  KEY `fk_rolesPermissions_roleName_idx` (`roleName`),
  KEY `fk_rolesPermissions_resourceName_idx` (`resourceName`),
  KEY `fk_rolesPermissions_resourceVersionNumber_idx` (`resourceVersionNumber`),
  KEY `fk_rolesPermissions_roleToken_idx` (`roleToken`),
  KEY `fk_rolesPermissions_resourceToken_idx` (`resourceToken`),
  KEY `fk_rolesPermissions_signerToken_idx` (`signerToken`),
  CONSTRAINT `fk_rolesPermissions_roleName`  FOREIGN KEY (`roleName`)  REFERENCES `roles` (`roleName`),
  CONSTRAINT `fk_rolesPermissions_resourceNameResourceVersionNumber`  FOREIGN KEY (`resourceName`, `resourceVersionNumber`)  REFERENCES `resources` (`resourceName`, `resourceVersionNumber`),
  CONSTRAINT `fk_rolesPermissions_roleToken` FOREIGN KEY (`roleToken`) REFERENCES `roles` (`roleToken`) ON UPDATE CASCADE,
  CONSTRAINT `fk_rolesPermissions_resourceToken` FOREIGN KEY (`resourceToken`) REFERENCES `resources` (`resourceToken`) ON UPDATE CASCADE,
  CONSTRAINT `fk_rolesPermissions_signerToken` FOREIGN KEY (`signerToken`) REFERENCES `users` (`userToken`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/* note how users can modify ONLY their public key and ONLY once */
CREATE VIEW `user_specific_users` (`asymEncPublicKey`, `asymSigPublicKey`,  `userToken`, `status`) AS
    SELECT
        `users`.`asymEncPublicKey` AS `asymEncPublicKey`,
        `users`.`asymSigPublicKey` AS `asymSigPublicKey`,
        `users`.`userToken` AS `userToken`,
        `users`.`status` AS `status`
    FROM
        `users`
    WHERE
        `users`.`username` = (CONVERT( SUBSTRING_INDEX(USER(), '@', 1) USING UTF8MB4)) AND
        `users`.`asymEncPublicKey` = 'mock' AND
        `users`.`asymSigPublicKey` = 'mock';



/* this view allows users to check their status */
CREATE VIEW `user_specific_users_status` (`status`) AS
    (
        SELECT
            `users`.`status` AS `status`
        FROM
            `users`
        WHERE
            `users`.`username` = (CONVERT( SUBSTRING_INDEX(USER(), '@', 1) USING UTF8MB4))
    )
    UNION
    (
        SELECT
            `deletedUsers`.`status` AS `status`
        FROM
            `deletedUsers`
        WHERE
            `deletedUsers`.`username` = (CONVERT( SUBSTRING_INDEX(USER(), '@', 1) USING UTF8MB4))
    );


/* users can access only their roles */
CREATE VIEW `user_specific_usersRoles`
(`username`, `roleName`, `userVersionNumber`, `roleVersionNumber`, `encryptedAsymEncPublicKey`,
 `encryptedAsymEncPrivateKey`, `encryptedAsymSigPublicKey`,
 `encryptedAsymSigPrivateKey`, `signature`) AS
    SELECT
        `usersRoles`.`username` AS `username`,
        `usersRoles`.`roleName` AS `roleName`,
        `usersRoles`.`userVersionNumber` AS `userVersionNumber`,
        `usersRoles`.`roleVersionNumber` AS `roleVersionNumber`,
        `usersRoles`.`encryptedAsymEncPublicKey` AS `encryptedAsymEncPublicKey`,
        `usersRoles`.`encryptedAsymEncPrivateKey` AS `encryptedAsymEncPrivateKey`,
        `usersRoles`.`encryptedAsymSigPublicKey` AS `encryptedAsymSigPublicKey`,
        `usersRoles`.`encryptedAsymSigPrivateKey` AS `encryptedAsymSigPrivateKey`,
        `usersRoles`.`signature` AS `signature`
    FROM
        `usersRoles`
    WHERE
        `usersRoles`.`username` = (CONVERT( SUBSTRING_INDEX(USER(), '@', 1) USING UTF8MB4));

/* users can access only the permissions they have */
CREATE VIEW `user_specific_rolesPermissions`
(`roleName`, `resourceName`, `roleToken`, `resourceToken`, `encryptedSymKey`,
 `roleVersionNumber`, `resourceVersionNumber`, `operation`, `signerToken`, `signature`) AS
    SELECT
        `rolesPermissions`.`roleName` AS `roleName`,
        `rolesPermissions`.`resourceName` AS `resourceName`,
        `rolesPermissions`.`roleToken` AS `roleToken`,
        `rolesPermissions`.`resourceToken` AS `resourceToken`,
        `rolesPermissions`.`encryptedSymKey` AS `encryptedSymKey`,
        `rolesPermissions`.`roleVersionNumber` AS `roleVersionNumber`,
        `rolesPermissions`.`resourceVersionNumber` AS `resourceVersionNumber`,
        `rolesPermissions`.`operation` AS `operation`,
        `rolesPermissions`.`signerToken` AS `signerToken`,
        `rolesPermissions`.`signature` AS `signature`
    FROM
        `rolesPermissions`
    WHERE
        `rolesPermissions`.`roleName` IN (SELECT `user_specific_usersRoles`.`roleName` FROM `user_specific_usersRoles`);


/* users can access only the resources they have permission over */
CREATE VIEW `user_specific_resources`
(`resourceName`, `resourceToken`, `resourceVersionNumber`, `reEncryptionThresholdNumber`, `status`, `enforcement`, `encryptedSymKey`) AS
    SELECT
        `resources`.`resourceName` AS `resourceName`,
        `resources`.`resourceToken` AS `resourceToken`,
        `resources`.`resourceVersionNumber` AS `resourceVersionNumber`,
        `resources`.`reEncryptionThresholdNumber` AS `reEncryptionThresholdNumber`,
        `resources`.`status` AS `status`,
        `resources`.`enforcement` AS `enforcement`,
        `resources`.`encryptedSymKey` AS `encryptedSymKey`
    FROM
        `resources`
    WHERE
        `resources`.`resourceName` IN (SELECT `user_specific_rolesPermissions`.`resourceName` FROM `user_specific_rolesPermissions`);


/* Avoid new users with same name as deleted ones */
DELIMITER //
CREATE TRIGGER uniqueUsernameAndTokenInsert BEFORE INSERT ON users
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedUsers WHERE username = NEW.username OR userToken = NEW.userToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'User was deleted';
 END IF;
END //

DELIMITER //
CREATE TRIGGER uniqueUsernameAndTokenUpdate BEFORE UPDATE ON users
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedUsers WHERE username = NEW.username OR userToken = NEW.userToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'User was deleted';
 END IF;
END //

/* Avoid new roles with same name as deleted ones */
DELIMITER //
CREATE TRIGGER uniqueRoleNameAndTokenInsert BEFORE INSERT ON roles
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedRoles WHERE roleName = NEW.roleName OR roleToken = NEW.roleToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'Role was deleted';
 END IF;
END //

DELIMITER //
CREATE TRIGGER uniqueRoleNameAndTokenUpdate BEFORE UPDATE ON roles
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedRoles WHERE roleName = NEW.roleName OR roleToken = NEW.roleToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'Role was deleted';
 END IF;
END //

/* Avoid new resources with same name as deleted ones */
DELIMITER //
CREATE TRIGGER uniqueResourceNameAndTokenInsert BEFORE INSERT ON resources
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedResources WHERE resourceName = NEW.resourceName OR resourceToken = NEW.resourceToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'Resource was deleted';
 END IF;
END //

DELIMITER //
CREATE TRIGGER uniqueResourceNameAndTokenUpdate BEFORE UPDATE ON resources
FOR EACH ROW BEGIN
 DECLARE c INT;
 SELECT COUNT(*) INTO c FROM deletedResources WHERE resourceName = NEW.resourceName OR resourceToken = NEW.resourceToken;
 IF (c > 0) THEN
   signal sqlstate '45000' SET MESSAGE_TEXT = 'Resource was deleted';
 END IF;
END //
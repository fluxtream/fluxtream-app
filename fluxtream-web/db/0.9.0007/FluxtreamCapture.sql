DROP TABLE IF EXISTS `Facet_FluxtreamCapturePhoto`;

CREATE TABLE `Facet_FluxtreamCapturePhoto` (
  `id`                  BIGINT(20) NOT NULL AUTO_INCREMENT,
  `guestId`             BIGINT(20) NOT NULL,
  `api`                 INT(11)    NOT NULL,
  `objectType`          INT(11)    NOT NULL,
  `hash`                CHAR(64)   NOT NULL,
  `title`               VARCHAR(255) DEFAULT NULL,
  `comment`             LONGTEXT,
  `fullTextDescription` LONGTEXT,
  `start`               BIGINT(20) NOT NULL,
  `end`                 BIGINT(20) NOT NULL,
  `timeUpdated`         BIGINT(20) NOT NULL,
  `captureYYYYDDD`      CHAR(7)    NOT NULL,
  `imageType`           CHAR(3)    NOT NULL,
  `orientation`         INT(11)    NOT NULL,
  `thumbnail0`          BLOB       NOT NULL,
  `thumbnail1`          BLOB       NOT NULL,
  `thumbnail2`          BLOB       NOT NULL,
  `thumbnail0Width`     INT(11)    NOT NULL,
  `thumbnail0Height`    INT(11)    NOT NULL,
  `thumbnail1Width`     INT(11)    NOT NULL,
  `thumbnail1Height`    INT(11)    NOT NULL,
  `thumbnail2Width`     INT(11)    NOT NULL,
  `thumbnail2Height`    INT(11)    NOT NULL,
  `isEmpty`             CHAR(1)    NOT NULL,
  `tags`                LONGTEXT,

  `latitude`            DOUBLE DEFAULT NULL,
  `longitude`           DOUBLE DEFAULT NULL,
  `heading`             FLOAT DEFAULT NULL,
  `headingRef`          CHAR(2) DEFAULT NULL,
  `altitude`            FLOAT DEFAULT NULL,
  `altitudeRef`         INT(11) DEFAULT NULL,
  `gpsPrecision`        FLOAT DEFAULT NULL,
  `gpsDatestamp`        VARCHAR(255) DEFAULT NULL,
  `gpsTimestamp`        VARCHAR(255) DEFAULT NULL,

  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `end_index` (`end`),
  KEY `guestId_index` (`guestId`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `objectType_index` (`objectType`),
  KEY `start_index` (`start`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `hash_index` (`hash`)
)
  ENGINE = MyISAM
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = latin1;

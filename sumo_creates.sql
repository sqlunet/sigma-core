-- FILES

CREATE TABLE ${files.table} (
  ${files.fileid} INT NOT NULL,
  ${files.file} VARCHAR(128) CHARACTER SET utf8mb4 NOT NULL,
  ${files.version} VARCHAR(5) CHARACTER SET utf8mb4 DEFAULT NULL,
  ${files.date} DATETIME DEFAULT NULL
);

ALTER TABLE ${files.table} ADD CONSTRAINT `pk_@{files.table}_@{files.fileid}` PRIMARY KEY (${files.fileid});
ALTER TABLE ${files.table} ADD CONSTRAINT `uk_@{files.table}_@{files.file}` UNIQUE KEY (${files.file});

-- FORMULAS

CREATE TABLE ${formulas.table} (
  ${formulas.formulaid} INT NOT NULL,
  ${formulas.formula} MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL,
  ${formulas.fileid} INT NOT NULL
);

ALTER TABLE ${formulas.table} ADD CONSTRAINT `pk_@{formulas.table}_@{formulas.formulaid}` PRIMARY KEY (${formulas.formulaid});
ALTER TABLE ${formulas.table} ADD KEY `k_@{formulas.table}_@{formulas.formula}` (${formulas.formula}(128));
ALTER TABLE ${formulas.table} ADD KEY `k_@{formulas.table}_@{formulas.fileid}` (${formulas.fileid});
ALTER TABLE ${formulas.table} ADD CONSTRAINT `fk_@{formulas.table}_@{formulas.fileid}` FOREIGN KEY (${formulas.fileid}) REFERENCES ${files.table} (${files.fileid});

-- FORMULAS_ARGS

CREATE TABLE ${formulas_args.table} (
  ${formulas_args.formulaid} INT NOT NULL,
  ${formulas_args.termid} INT NOT NULL,
  ${formulas_args.parsetype} ENUM('a','s','p','c') NOT NULL,
  ${formulas_args.argnum} INT DEFAULT NULL
);

ALTER TABLE ${formulas_args.table} ADD KEY `k_@{formulas_args.table}_@{formulas_args.formulaid}` (${formulas_args.formulaid});
ALTER TABLE ${formulas_args.table} ADD KEY `k_@{formulas_args.table}_@{formulas_args.termid}` (${formulas_args.termid});
ALTER TABLE ${formulas_args.table} ADD KEY `k_@{formulas_args.table}_@{formulas_args.parsetype}` (${formulas_args.parsetype});
ALTER TABLE ${formulas_args.table} ADD CONSTRAINT `fk_@{formulas_args.table}_@{formulas_args.formulaid}` FOREIGN KEY (${formulas_args.formulaid}) REFERENCES ${formulas.table} (${formulas.formulaid});
ALTER TABLE ${formulas_args.table} ADD CONSTRAINT `fk_@{formulas_args.table}_@{formulas_args.termid}` FOREIGN KEY (${formulas_args.termid}) REFERENCES ${terms.table} (${terms.termid});

-- TERMS

CREATE TABLE ${terms.table} (
  ${terms.termid} INT NOT NULL,
  ${terms.term} VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
);

ALTER TABLE ${terms.table} ADD CONSTRAINT `pk_@{terms.table}_@{terms.termid}` PRIMARY KEY (${terms.termid});
ALTER TABLE ${terms.table} ADD CONSTRAINT `uk_@{terms.table}_@{terms.term}` UNIQUE KEY (${terms.term});

-- TERMS_ATTRS

CREATE TABLE ${terms_attrs.table} (
  ${terms_attrs.termid} INT NOT NULL,
  ${terms_attrs.attr} VARCHAR(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
);

ALTER TABLE ${terms_attrs.table} ADD CONSTRAINT `pk_@{terms_attrs.table}_@{terms_attrs.termid}_@{terms_attrs.attr}` PRIMARY KEY (${terms_attrs.termid},${terms_attrs.attr});
ALTER TABLE ${terms_attrs.table} ADD CONSTRAINT `fk_@{terms_attrs.table}_@{terms_attrs.termid}` FOREIGN KEY (${terms_attrs.termid}) REFERENCES ${terms.table} (${terms.termid});

-- TERMS_SENSES

CREATE TABLE ${terms_senses.table} (
  ${terms_senses.termid}  INT NOT NULL,
  ${terms_senses.rel} ENUM('=','+','@',':','[',']') NOT NULL,
  ${terms_senses.synsetid} INT NOT NULL
);

ALTER TABLE ${terms_senses.table} ADD CONSTRAINT `pk_@{terms_senses.table}_@{terms_senses.synsetid}` PRIMARY KEY (${terms_senses.synsetid});
ALTER TABLE ${terms_senses.table} ADD KEY `k_@{terms_senses.table}_@{terms_senses.termid}` (${terms_senses.termid});
ALTER TABLE ${terms_senses.table} ADD KEY `k_@{terms_senses.table}_@{terms_senses.rel}` (${terms_senses.rel});

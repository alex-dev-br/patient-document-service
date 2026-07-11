CREATE TABLE patients (
                          id UUID PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          birth_date DATE NOT NULL,
                          cpf VARCHAR(14),
                          email VARCHAR(150),
                          phone VARCHAR(30),
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP
);

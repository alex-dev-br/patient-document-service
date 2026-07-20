# ==========================================
# Estágio 1: Compilação (Build)
# ==========================================
FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder

WORKDIR /app

# Copia primeiro o arquivo de dependências para aproveitar o cache
COPY pom.xml .

# Baixa as dependências sem compilar o código
RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY src ./src

# Gera o arquivo executável da aplicação
RUN mvn clean package -DskipTests

# ==========================================
# Estágio 2: Execução (Runtime)
# ==========================================
FROM eclipse-temurin:25-jre-alpine-3.23

WORKDIR /app

# Cria usuário e grupo exclusivos, sem privilégios administrativos
RUN addgroup -S appgroup && adduser -S -D -H -G appgroup appuser

# Copia somente o artefato final, atribuindo-o ao usuário da aplicação
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

EXPOSE 8080

USER appuser:appgroup

ENTRYPOINT ["java", "-jar", "app.jar"]

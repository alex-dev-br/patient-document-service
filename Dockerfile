# ==========================================
# Estágio 1: Compilação (Build)
# ==========================================
FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia apenas o arquivo de configuração de dependências primeiro (otimiza o cache do Docker)
COPY pom.xml .

# Baixa as dependências do projeto sem compilar o código (cache de camadas)
RUN mvn dependency:go-offline -B

# Copia a pasta de código fonte para o container
COPY src ./src

# Executa o build da aplicação gerando o arquivo .jar (ignora os testes para acelerar o processo)
RUN mvn clean package -DskipTests

# ==========================================
# Estágio 2: Execução (Run)
# ==========================================
FROM eclipse-temurin:25-jre-alpine-3.23

# Define o diretório onde o jar final vai rodar
WORKDIR /app

# Copia o arquivo .jar gerado no estágio anterior (builder) para esta nova imagem limpa
COPY --from=builder /app/target/*.jar app.jar

# Informa a porta que a aplicação escuta
EXPOSE 8080

# Define um usuário não-root por questões de segurança
USER 1000

# Comando para iniciar a aplicação Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]

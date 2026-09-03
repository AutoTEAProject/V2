COMPOSE = docker compose -f infra/docker-compose.yml

.PHONY: all re clean fclean

all:
	$(COMPOSE) up -d --build

re: fclean all

clean:
	$(COMPOSE) down

fclean:
	$(COMPOSE) down -v --rmi local

package com.example;

import java.util.*;

public class Main {

    // Класс для представления игрока с информацией о нём
    static class PlayerInfo {
        double rating;           // Текущий рейтинг доверия
        List<Integer> accusedBy; // Списки, кто меня подозревал
        List<Integer> suspected; // Кого я подозреваю
        List<Integer> trusted;   // Кого считаю мирным
        boolean isDead;          // Жив ли игрок?
        boolean isMafia;         // Является ли мафиози?
        boolean isCivilian;      // Мирный ли игрок?

        public PlayerInfo(double initialRating) {
            this.rating = initialRating;
            this.accusedBy = new ArrayList<>();
            this.suspected = new ArrayList<>();
            this.trusted = new ArrayList<>();
            this.isDead = false;
            this.isMafia = false;
            this.isCivilian = false;
        }
    }

    // Класс для сохранения статуса игры
    static class GameState {
        List<PlayerInfo> players;         // Информация о каждом игроке
        List<Integer> livingPlayers;      // Номера выживших игроков
        List<Integer> confirmedMafias;    // Точно известные мафиози
        List<Integer> confirmedCivilians; // Точно известные мирные жители
        List<List<Integer>> votingHistory; // История голосований

        public GameState(int numPlayers) {
            players = new ArrayList<>(numPlayers);
            for (int i = 0; i < numPlayers; i++) {
                players.add(new PlayerInfo(0.5)); // Начальный рейтинг доверия 0.5
            }
            livingPlayers = new ArrayList<>(players.size()); // Изначально все живы
            for (int i = 0; i < numPlayers; i++) {
                livingPlayers.add(i);
            }
            confirmedMafias = new ArrayList<>();
            confirmedCivilians = new ArrayList<>();
            votingHistory = new ArrayList<>();
        }

        // Метод для добавления или удаления игроков из списка живых
        public void changeLivingStatus(int playerId) {
            livingPlayers.removeIf(p -> p.equals(playerId));
        }

        // Установка точного статуса игрока (мафия или мирный)
        public void confirmRole(int playerId, boolean isMafia) {
            if (isMafia) {
                confirmedMafias.add(playerId);
                players.get(playerId).rating = 0; // Устанавливаем рейтинг мафиози на 0
                players.get(playerId).isMafia = true;
            } else {
                confirmedCivilians.add(playerId);
                players.get(playerId).rating = 1; // Устанавливаем рейтинг мирного на 1
                players.get(playerId).isCivilian = true;
            }
        }

        // Добавление истории голосования
        public void recordVotingResults(List<Integer> votes) {
            votingHistory.add(votes);
        }
    }

    // Основная логика обновления рейтингов
    public static void setRatings(GameState gameState) {
        List<PlayerInfo> players = gameState.players;
        List<Integer> livingPlayers = gameState.livingPlayers;

        // Перебираем всех активных игроков
        for (PlayerInfo player : players) {
            // Обрабатываем мирных игроков
            double penalty = 0;
            Boolean flag = false;
            for (Integer trustedPlayerId : player.trusted) {
                flag = true;
                penalty = 0.5 / livingPlayers.size() / player.trusted.size();
                players.get(trustedPlayerId).rating += penalty; // Повышение рейтинга доверенного
            }
            if(flag == true)  {
                player.rating -= penalty;              // Снижение собственного рейтинга
                flag = false;
            }

            // Обрабатываем подозреваемых
            for (Integer suspectedPlayerId : player.suspected) {
                flag = true;
                penalty = 0.5 / livingPlayers.size() / player.suspected.size();
                players.get(suspectedPlayerId).rating -= penalty; // Понижение рейтинга подозреваемого
            }
            if(flag == true)  {
                player.rating -= penalty;              // Снижение собственного рейтинга
            }
        }


    }

    public static void bonusRating(GameState gameState) {
        List<PlayerInfo> players = gameState.players;
        List<Integer> confirmedMafias = gameState.confirmedMafias;
        // Дополнительно повышаем рейтинг тех, кто правильно указал на мафию
        for (Integer correctAccuser : confirmedMafias) {
            PlayerInfo accuser = players.get(correctAccuser);
            for (Integer suspectedPlayerId : accuser.suspected) {
                if (gameState.confirmedMafias.contains(suspectedPlayerId)) {
                    double bonus = 0.5 * Math.abs(accuser.rating - players.get(suspectedPlayerId).rating);
                    accuser.rating += bonus;
                }
            }
        }
    }

    // Генерация всех возможных троек мафий из набора чисел
    private static List<int[]> generateCombinations(int n, int k) {
        List<int[]> combinations = new ArrayList<>();
        int[] currentCombination = new int[k];
        backtrack(combinations, currentCombination, 0, 0, n);
        return combinations;
    }

    // Рекурсивная функция для генерации комбинаций методом перебора
    private static void backtrack(List<int[]> result, int[] current, int start, int depth, int total) {
        if (depth == current.length) {
            result.add(current.clone());
            return;
        }
        for (int i = start; i <= total - (current.length - depth); ++i) {
            current[depth] = i;
            backtrack(result, current, i + 1, depth + 1, total);
        }
    }

    // Фильтрация недопустимых комбинаций
    public static List<int[]> filterInvalidCombinations(GameState gameState, List<int[]> combinations) {
        List<int[]> validCombinations = new ArrayList<>();
        List<Integer> confirmedCivilians = gameState.confirmedCivilians;

        for (int[] combo : combinations) {
            boolean invalidCombo = false;

            // Исключаем тройки, содержащие мирных игроков
            for (int playerId : combo) {
                if (confirmedCivilians.contains(playerId)) {
                    invalidCombo = true;
                    break;
                }
            }

            if (!invalidCombo) {
                validCombinations.add(combo);
            }
        }

        return validCombinations;
    }

    // Главный метод расчета наилучшей гипотезы
    public static int[] findMinimalMafiaSet(GameState gameState) {
        List<PlayerInfo> players = gameState.players;
        List<Integer> livingPlayers = gameState.livingPlayers;
        List<int[]> possibleSets = generateCombinations(livingPlayers.size(), 3); // Возможные тройки мафии

        // Исключаем недостоверные комбинации
        List<int[]> filteredSets = filterInvalidCombinations(gameState, possibleSets);


        // Формируем гипотезы, основанные на текущих рейтингах
        Map<String, Double> hypothesisScores = new HashMap<>();
        for (int[] combination : filteredSets) {
            double combinedScore = 0;
            for (int id : combination) {
                combinedScore += players.get(id).rating;
            }
            hypothesisScores.put(Arrays.toString(combination), combinedScore);
        }

        // Подробный вывод информации по каждой гипотезе
        System.out.println("\nПодробная информация по каждой гипотезе:");
        hypothesisScores.forEach((hypothesis, score) ->
                System.out.println("Группа мафии: " + hypothesis + ", Общая сумма рейтингов: " + score)
        );

        // Лучший выбор на основе минимальной суммы рейтингов
        String bestKey = Collections.min(hypothesisScores.entrySet(),
                        Comparator.comparingDouble(Map.Entry::getValue))
                .getKey();
        double minimalRating = hypothesisScores.get(bestKey);
        System.out.println("\nИтоговый минимальный рейтинг группы мафии: " + minimalRating);

        return Arrays.stream(bestKey.substring(1, bestKey.length() - 1).split(", "))
                .mapToInt(Integer::parseInt)
                .toArray();
    }


    // Основной метод
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final int NUM_PLAYERS = 10; // Общее количество игроков
        GameState gameState = new GameState(NUM_PLAYERS); // Создаем состояние игры

        // Первоначальный опрос о предпочтениях игроков
        for (int i = 0; i < NUM_PLAYERS; i++) {
            System.out.print("Количество подозреваемых для игрока №" + (i + 1) + ": ");
            int numSuspects = scanner.nextInt();
            int[] suspects = new int[numSuspects];
            for (int j = 0; j < numSuspects; j++) {
                System.out.print("Подозреваешь игрока №");
                suspects[j] = scanner.nextInt() - 1;
            }

            System.out.print("Количество мирных игроков для игрока №" + (i + 1) + ": ");
            int numTrusted = scanner.nextInt();
            int[] trusted = new int[numTrusted];
            for (int j = 0; j < numTrusted; j++) {
                System.out.print("Мирный игрок №");
                trusted[j] = scanner.nextInt() - 1;
            }

            // Добавляем данные о подозрениях и доверии
            for (int suspect : suspects) {
                gameState.players.get(i).suspected.add(suspect);
            }
            for (int trustedPlayer : trusted) {
                gameState.players.get(i).trusted.add(trustedPlayer);
            }
        }
        // установка рейтинга на основании мнений
        setRatings(gameState);

        // Интерфейс командного меню
        while (true) {
            System.out.println("\nМеню:");
            System.out.println("1. Провести голосование.");
            System.out.println("2. Назначить игрока точно мафиози или мирным.");
            System.out.println("3. Объявить игрока убитым.");
            System.out.println("4. Вывести текущий статус игры.");
            System.out.println("5. Вывести наилучшую гипотезу о мафии.");
            System.out.println("6. Выход.");
            System.out.print("Ваш выбор: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("Проведено голосование.");
                    // Здесь должна быть логика голосования, если хотите дополнить
                    break;
                case 2:
                    System.out.print("ID игрока: ");
                    int playerId = scanner.nextInt();
                    System.out.print("Мафия (1)/Мирный (0)? ");
                    boolean isMafia = scanner.nextInt() == 1;
                    gameState.confirmRole(playerId, isMafia);
                    break;
                case 3:
                    System.out.print("ID игрока, объявленного убитым: ");
                    int deadPlayerId = scanner.nextInt();
                    gameState.changeLivingStatus(deadPlayerId);
                    break;
                case 4:
                    System.out.println("Список живых игроков: " + gameState.livingPlayers);
                    System.out.println("Установленные мафиози: " + gameState.confirmedMafias);
                    System.out.println("Установленные мирные жители: " + gameState.confirmedCivilians);
                    break;
                case 5:
                    int i = 0;
                    for(PlayerInfo player : gameState.players) {
                        i++;
                        System.out.println("Рейтинг игрока №" + i + " = " + player.rating);
                    }
                    int[] mafiaSet = findMinimalMafiaSet(gameState);
                    System.out.println("\nЛучший набор мафиози: " + Arrays.toString(mafiaSet));
                    break;
                case 6:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Некорректный выбор.");
            }
        }
    }

}
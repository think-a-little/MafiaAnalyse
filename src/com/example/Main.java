package com.example;

import java.util.*;

public class Main {

    // Класс для представления игрока с информацией о нём
    static class PlayerInfo {
        double rating;           // Текущий рейтинг доверия
        List<Integer> accusedBy; // Списки, кто меня подозревал
        List<Integer> suspected; // Кого я подозреваю
        List<Integer> trusted;   // Кого считаю мирным

        public PlayerInfo(double initialRating) {
            this.rating = initialRating;
            this.accusedBy = new ArrayList<>();
            this.suspected = new ArrayList<>();
            this.trusted = new ArrayList<>();
        }
    }

    // Класс для сохранения статуса игры
    static class GameState {
        List<PlayerInfo> players;     // Информация о каждом игроке
        List<Integer> livingPlayers; // Номера выживших игроков
        List<Integer> confirmedMafias; // Точно известные мафиози
        List<Integer> confirmedCivilians; // Точно известные мирные жители

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
            } else {
                confirmedCivilians.add(playerId);
                players.get(playerId).rating = 1; // Устанавливаем рейтинг мирного на 1
            }
        }

    }

    // Основная логика обновления рейтингов
    public static void updateRatings(GameState gameState) {
        List<PlayerInfo> players = gameState.players;
        List<Integer> livingPlayers = gameState.livingPlayers;
        List<Integer> confirmedMafias = gameState.confirmedMafias;
        List<Integer> confirmedCivilians = gameState.confirmedCivilians;

        // Перебираем всех активных игроков
        for (PlayerInfo player : players) {
            // Обрабатываем мирных игроков
            for (Integer trustedPlayerId : player.trusted) {
                double trustPenalty = 0.5 / livingPlayers.size() / player.trusted.size();
                player.rating -= trustPenalty;              // Снижение собственного рейтинга
                players.get(trustedPlayerId).rating += trustPenalty; // Повышение рейтинга доверенного
            }

            // Обрабатываем подозреваемых
            for (Integer suspectedPlayerId : player.suspected) {
                double suspicionPenalty = 0.5 / livingPlayers.size() / player.suspected.size();
                player.rating -= suspicionPenalty;                  // Снижение собственного рейтинга
                players.get(suspectedPlayerId).rating -= suspicionPenalty; // Понижение рейтинга подозреваемого
            }
        }

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

    // Главный метод расчета наилучшей гипотезы
    public static int[] findMinimalMafiaSet(GameState gameState) {
        List<PlayerInfo> players = gameState.players;
        List<Integer> livingPlayers = gameState.livingPlayers;
        List<int[]> possibleSets = generateCombinations(livingPlayers.size(), 3); // Возможные тройки мафии

        // Формируем гипотезы, основанные на текущих рейтингах
        Map<String, Double> hypothesisScores = new HashMap<>();
        for (int[] combination : possibleSets) {
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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final int NUM_PLAYERS = 10; // Общее количество игроков
        GameState gameState = new GameState(NUM_PLAYERS); // Создаем состояние игры

        // Интерактивный сбор данных от пользователя
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

        // Обновление рейтингов
        updateRatings(gameState);

        // Пример изменения живого статуса игрока
        gameState.changeLivingStatus(5); // Удалили игрока #5 из живых

        gameState.confirmRole(5, false); // Установили игрока #3 как мафиози

        // Пример подтверждения игрока как мафиози
        gameState.confirmRole(3, true); // Установили игрока #3 как мафиози

        // Пример подтверждения игрока как мирного
        gameState.confirmRole(7, false); // Установили игрока #7 как мирного

        // Получение наилучшего предположения о группе мафии
        int[] minimalMafiaSet = findMinimalMafiaSet(gameState);
        System.out.println("\nМинимальная группа мафии: " + Arrays.toString(minimalMafiaSet));

    }
}
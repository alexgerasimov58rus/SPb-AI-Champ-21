package spb_ai_champ;

import spb_ai_champ.model.*;
import java.util.*;


public class MyStrategy {

    static final private int MIN_NEED_ROBOTS_ON_PLANET = 20;
    static final private int MIN_NEED_COUNT_RESOURCE_ON_PLANET = 3;
    static final private int LEAVE_ROBOTS = 5;

    public Action getAction(Game game) {
        List<MoveAction> moveActionList = new ArrayList<>();
        List<BuildingAction> buildingActionList = new ArrayList<>();

        // список планет, которые производят сырой ресурс
        List<Planet> rawResourcePlanets = getRawResourcePlanets(game);

        // список планет с моими роботами (сортированная от большего числа моих роботов к меньшему)
        List<Planet> planetsWithMyRobots = getPlanetsWithMyRobots(game);

        for(Planet p: planetsWithMyRobots){
            // если на rawResourcePlanets есть мои роботы
            if( planetsWithMyRobots.contains(p)){
                // если на этой планете нет здания, строю
                if( p.getBuilding() == null){
                    buildingActionList.add(
                        new BuildingAction(p.getId(), harvestResourceToBuildingType(p.getHarvestableResource()))
                    );
                }
            }
        }

        // ищу планету, на которой есть мои роботы и есть камень
        for(Planet p: planetsWithMyRobots){
            if( p.getResources().containsKey(Resource.STONE) && p.getBuilding() != null){
                // из rawResourcePlanets ищу ближайшую планету, на которой нет здания
                Planet rawResourcePlanetWithoutBuilding = getNearestPlanetWithoutBuilding(p, rawResourcePlanets);
                if( rawResourcePlanetWithoutBuilding != null){
                    // нашел, отправляю туда робота
                    moveActionList.add(
                        new MoveAction(
                                p.getId(),
                                rawResourcePlanetWithoutBuilding.getId(),
                                LEAVE_ROBOTS,
                                Resource.STONE
                        )
                    );
                }
            }
        }

        return new Action(
                moveActionsListToArray(moveActionList),
                buildingActionsListToArray(buildingActionList));
    }

    private MoveAction[] moveActionsListToArray(List<MoveAction> moveActionList)
    {
        MoveAction[] result = new MoveAction[moveActionList.size()];
        for(int i = 0; i < moveActionList.size(); i++) {
            result[i] = moveActionList.get(i);
        }
        return result;
    }

    private BuildingAction[] buildingActionsListToArray(List<BuildingAction> buildingActionList)
    {
        BuildingAction[] result = new BuildingAction[buildingActionList.size()];
        for(int i = 0; i < buildingActionList.size(); i++) {
            result[i] = buildingActionList.get(i);
        }
        return result;
    }

    private BuildingType harvestResourceToBuildingType(Resource r)
    {
        switch (r){
            case STONE: return BuildingType.QUARRY;
            case ORE: return BuildingType.MINES;
            case SAND: return BuildingType.CAREER;
            case ORGANICS: return BuildingType.FARM;
        }

        return BuildingType.QUARRY;
    }

    private List<Planet> getRawResourcePlanets(Game game)
    {
        List<Planet> result = new ArrayList<>();
        Planet[] planets = game.getPlanets();

        for(int i = 0; i < planets.length; i++) {
            if( planets[i].getHarvestableResource() != null){
                result.add(planets[i]);
            }
        }
        return result;
    }

    private List<Planet> getPlanetsWithMyRobots(Game game)
    {
        List<Planet> result = new ArrayList<>();
        Planet[] planets = game.getPlanets();

        for(int i = 0; i < planets.length; i++) {
            int myCount = getMyRobotsOnPlanet(planets[i], game);
            int opCount = getOpponentsRobotsOnPlanet(planets[i], game);

            if( myCount - opCount > MIN_NEED_ROBOTS_ON_PLANET){
                result.add(planets[i]);
            }
        }

        Collections.sort(result, new Comparator<Planet>() {
            @Override
            public int compare(Planet o1, Planet o2) {
                int countO1 = getMyRobotsOnPlanet(o1, game) - getOpponentsRobotsOnPlanet(o1, game);
                int countO2 = getMyRobotsOnPlanet(o2, game) - getOpponentsRobotsOnPlanet(o2, game);

                return countO2 - countO1;
            }
        });

        return result;
    }

    private int getMyRobotsOnPlanet(Planet p, Game game)
    {
        int result = 0;
        WorkerGroup[] groups = p.getWorkerGroups();
        for(int i = 0; i < groups.length; i++) {
            if (groups[i].getPlayerIndex() == game.getMyIndex())
                result += groups[i].getNumber();
        }

        return result;
    }

    private int getOpponentsRobotsOnPlanet(Planet p, Game game)
    {
        int result = 0;
        WorkerGroup[] groups = p.getWorkerGroups();
        for(int i = 0; i < groups.length; i++) {
            if (groups[i].getPlayerIndex() != game.getMyIndex())
                result += groups[i].getNumber();
        }

        return result;
    }

    private Planet getNearestPlanetWithoutBuilding(Planet myPlanet, List<Planet> rawResourcePlanets)
    {
        Planet result = null;
        double minDist = 1e9;

        for(Planet p: rawResourcePlanets){
            if( p != myPlanet && p.getBuilding() == null){
                double dist = (myPlanet.getX() - p.getX()) * (myPlanet.getX() - p.getX()) +
                              (myPlanet.getY() - p.getY()) * (myPlanet.getY() - p.getY());

                if( dist < minDist){
                    minDist = dist;
                    result = p;
                }
            }
        }

        return result;
    }
}
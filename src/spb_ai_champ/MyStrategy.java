package spb_ai_champ;

import spb_ai_champ.model.*;
import java.util.*;


public class MyStrategy {

    static final private int MIN_NEED_ROBOTS_ON_PLANET = 20;
    static final private int MIN_NEED_COUNT_RESOURCE_ON_PLANET = 3;

    public Action getAction(Game game) {
        List<MoveAction> moveActionList = new ArrayList<>();
        List<BuildingAction> buildingActionList = new ArrayList<>();

        // планеты, где необходимы мои роботы
        Map<Planet, Integer> needRobotsPlanets = getNeedRobotPlanets(game);

        // планеты, где есть мои роботы
        Map<Planet, Integer> planetsWithMyRobots = getPlanetsWithMyRobots(game);

        // списки планет, где добывается ресурс
        Map<Resource, List<Planet>> planetsWithResourceBuilding = getPlanetsWithResourceBuilding(game);

        // списки планет, где нужны ресурсы для работы здания
        Map<Resource, List<Planet>> planetsNeedResource = getPlanetsNeedResource(game);

        // список текущих существующих построенных типов зданий
        Map<BuildingType, List<Planet>> existingBuildingTypes = getExistingBuildingTypes(game);

        // список планет, где нет зданий
        List<Planet> emptyPlanets = getEmptyPlanets(game, needRobotsPlanets);

        // планета, где больше всего моих роботов
        Planet maxMyRobotsPlanet = getMaxMyRobotsPlanet(planetsWithMyRobots);

        // ищу ближайшую к maxMyRobotsPlanet, где нет ни одного здания
        Planet emptyNearPlanet = getEmptyNearPlanet(maxMyRobotsPlanet, emptyPlanets);
        if( emptyNearPlanet != null){
            BuildingType buildingType = getNeedBuildingType(existingBuildingTypes);
            addPlanetToPlanetsNeedResource(game, buildingType, emptyNearPlanet, planetsNeedResource);
            moveActionList.add(
                new MoveAction(
                        maxMyRobotsPlanet.getId(),
                        emptyNearPlanet.getId(),
                        MIN_NEED_ROBOTS_ON_PLANET,
                        null)
            );

            buildingActionList.add(
                    new BuildingAction(
                        emptyNearPlanet.getId(), buildingType
                    )
            );

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

    private Map<Planet, Integer> getNeedRobotPlanets(Game game){
        Planet[] planets = game.getPlanets();
        Map<Planet, Integer> needRobotsPlanets = new HashMap<>();

        for(int i = 0; i < planets.length; i++) {
            if( planets[i].getBuilding() != null){
                WorkerGroup[] groups = planets[i].getWorkerGroups();
                int opCountRobots = 0;
                int myCountRobots = 0;
                for(int j = 0; j < groups.length; j++) {
                    if( groups[j].getPlayerIndex() == game.getMyIndex())
                        myCountRobots += groups[j].getNumber();
                    else
                        opCountRobots += groups[j].getNumber();
                }
                if( myCountRobots - opCountRobots < MIN_NEED_ROBOTS_ON_PLANET){
                    needRobotsPlanets.put(planets[i], MIN_NEED_ROBOTS_ON_PLANET - (myCountRobots - opCountRobots));
                }
            }
        }
        return needRobotsPlanets;
    }

    private Map<Planet, Integer> getPlanetsWithMyRobots(Game game){
        Planet[] planets = game.getPlanets();
        Map<Planet, Integer> result = new HashMap<>();

        for(int i = 0; i < planets.length; i++) {
            WorkerGroup[] groups = planets[i].getWorkerGroups();
            int opCountRobots = 0;
            int myCountRobots = 0;
            for(int j = 0; j < groups.length; j++) {
                if( groups[j].getPlayerIndex() == game.getMyIndex())
                    myCountRobots += groups[j].getNumber();
                else
                    opCountRobots += groups[j].getNumber();
            }
            result.put(planets[i], myCountRobots - opCountRobots);
        }
        return result;
    }

    private Map<Resource, List<Planet>> getPlanetsWithResourceBuilding(Game game)
    {
        Map<Resource, List<Planet>> result = new HashMap<>();
        Planet[] planets = game.getPlanets();
        for(int i = 0; i < planets.length; i++) {
            if (planets[i].getBuilding() != null) {
                BuildingProperties prop = game.getBuildingProperties().get(planets[i].getBuilding().getBuildingType());

                if(!result.containsKey(prop.getProduceResource())){
                    result.put(prop.getProduceResource(), new ArrayList<>());
                }

                result.get(prop.getProduceResource()).add(planets[i]);
            }
        }
        return result;
    }

    private Map<Resource, List<Planet>> getPlanetsNeedResource(Game game)
    {
        Map<Resource, List<Planet>> result = new HashMap<>();

        Planet[] planets = game.getPlanets();
        for(int i = 0; i < planets.length; i++) {
            if (planets[i].getBuilding() != null) {
                BuildingProperties prop = game.getBuildingProperties().get(planets[i].getBuilding().getBuildingType());
                Map<Resource, Integer> planetResources = planets[i].getResources();

                for(Map.Entry<Resource, Integer> pair: prop.getBuildResources().entrySet()){
                    int count = 0;
                    Resource resource = pair.getKey();
                    if( planetResources.containsKey(resource)){
                        count = planetResources.get(resource);
                    }

                    if( count < MIN_NEED_COUNT_RESOURCE_ON_PLANET){
                        if(!result.containsKey(resource)){
                            result.put(resource, new ArrayList<>());
                        }
                        result.get(resource).add(planets[i]);
                    }
                }

            }
        }
        return result;
    }

    private Planet getMaxMyRobotsPlanet(Map<Planet, Integer> myRobotsPlanets)
    {
        Planet result = null;

        for(Map.Entry<Planet, Integer> pair: myRobotsPlanets.entrySet()){
            if( result == null){
                result = pair.getKey();
            }
            else {
                if( myRobotsPlanets.get(result) < pair.getValue()){
                    result = pair.getKey();
                }
            }
        }
        return result;
    }

    private List<Planet> getEmptyPlanets(Game game, Map<Planet, Integer> needRobotsPlanets)
    {
        List<Planet> result = new ArrayList<>();
        Planet[] planets = game.getPlanets();
        for(int i = 0; i < planets.length; i++) {
            if (planets[i].getBuilding() == null && needRobotsPlanets.containsKey(planets[i])) {
                result.add(planets[i]);
            }
        }

        return result;
    }

    private Planet getEmptyNearPlanet(Planet maxMyRobotsPlanet, List<Planet> emptyPlanets)
    {
        Planet result = null;
        double minDist = 0.0;
        for(Planet p: emptyPlanets){
            if( result == null){
                result = p;
                minDist =
                        (maxMyRobotsPlanet.getX() - p.getX())*(maxMyRobotsPlanet.getX() - p.getX()) +
                        (maxMyRobotsPlanet.getY() - p.getY())*(maxMyRobotsPlanet.getY() - p.getY());
            }
            else{
                double dist =
                        (maxMyRobotsPlanet.getX() - p.getX())*(maxMyRobotsPlanet.getX() - p.getX()) +
                        (maxMyRobotsPlanet.getY() - p.getY())*(maxMyRobotsPlanet.getY() - p.getY());

                if( dist < minDist){
                    dist = minDist;
                    result = p;
                }
            }
        }
        return result;
    }

    private Map<BuildingType, List<Planet>> getExistingBuildingTypes(Game game)
    {
        Map<BuildingType, List<Planet>> result = new HashMap<>();

        Planet[] planets = game.getPlanets();
        for(int i = 0; i < planets.length; i++) {
            if (planets[i].getBuilding() != null) {
                BuildingType type = planets[i].getBuilding().getBuildingType();
                if(!result.containsKey(type)){
                    result.put(type, new ArrayList<>());
                }
                result.get(type).add(planets[i]);
            }
        }
        return result;
    }

    private BuildingType getNeedBuildingType(Map<BuildingType, List<Planet>> existingBuildingTypes)
    {
        BuildingType result = BuildingType.QUARRY;
        int minCountBuildingTypes = 1000000;
        BuildingType types[] = BuildingType.values();

        for(int i = 0; i < types.length; i++) {
            if( existingBuildingTypes.containsKey(types[i])){
                if( existingBuildingTypes.get(types[i]).size() < minCountBuildingTypes){
                    minCountBuildingTypes = existingBuildingTypes.get(types[i]).size();
                    result = types[i];
                }
            }else{
                result = types[i];
                break;
            }
        }
        return result;
    }

    private void addPlanetToPlanetsNeedResource(Game game, BuildingType buildingType, Planet planet, Map<Resource, List<Planet>> planetsNeedResource)
    {
        BuildingProperties prop = game.getBuildingProperties().get(buildingType);
        for(Map.Entry<Resource, Integer> pair: prop.getBuildResources().entrySet()){
            if(!planetsNeedResource.containsKey(pair.getKey())){
                planetsNeedResource.put(pair.getKey(), new ArrayList<>());
            }
            planetsNeedResource.get(pair.getKey()).add(planet);
        }
    }
}
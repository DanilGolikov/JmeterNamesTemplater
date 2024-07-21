Jmeter Names Templater

Jmeter Names Templater - это плагин для Jmeter, который позволяет задавать шаблоны наименования элементам и переименовывать их на основе описанного конфигурационного файла.

## Установка

1. Скачайте последнюю версию плагина с [GitHub Releases](#).
2. Скопируйте файл плагина в папку `lib/ext` вашего Jmeter.
3. Перезапустите Jmeter.

### Файл конфигурации

Файл `renameConfig.json` используется для описания шаблонов для элементов. Файл конфигурации находится в `jmeter/bin/rename-config.json`</br>
Пример структуры файла:
```json
{
    "debugEnable": true,
    "reloadAllTree": false,
    "removeEmptyVars": false,
    "replace": [
        ["null_", "", "HTTPSamplerProxy"],
        ["http_", ""]
    ],
    "NodeProperties": {
        "HTTPSamplerProxy": {
            "skipDisabled": false,
            "disableJmeterVars": true,
            "debugPrintConditionsResult": true,
            "search": [
                {
                    "searchIn": "#{name}",
                    "searchReg": "(VAR.*)",
                    "searchRegGroup": 1,
                    "searchOutVar": "global.testGlobalVar_2",
                    "leftRightSymbols": ["|", "|"],
                    "searchDefault": "null"
                }
            ],
            "conditions": [
                {
                    "inParentType": ["ThreadGroup"],
                    "strEquals": ["#{name}", "#{name}"],
                    "strContains": ["#{name}", "T"],
                    "minLevel": 2,
                    "maxLevel": 4,
                    "currentLevel": 2,
                    "leftRightSymbols": ["|", "|"],
                    "skip": false,
                    "counterCommands": "",
                    "putVar": ["global.testGlobalVar_2", "test"],
                    "template": "cond1_#{global.testGlobalVar_2}_#{__counter(current,,02)}"
                }
            ],
            "template": "#{global.testGlobalVar_2}_#{__counter(current,,02)}_#{protocol}_#{param.3}"
        }
    },
    "variables": {
        "testGlobalVar_1": "GLOBAL_VAR_1",
        "testGlobalVar_2": true,
        "testGlobalVar_3": 999
    },
    "counters": {
        "myCounter": {
            "startValue": 1,
            "endValue": 3,
            "increment": 1
        },
        "myCounter2": {
            "startValue": 15,
            "endValue": 30,
            "increment": 5
        }
    }
}
```

### Описание полей конфигурационного файла

| Поле                               | Тип    | Описание                                                                                                                          |
|------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------|
| `debugEnable`                      | Bool   | Отображать в логах полное дерево скрипта. По умолчанию - true.                                                                    |
| `reloadAllTree`                    | Bool   | Перезагружать после переименования полностью дерево. По умолчанию - false.                                                        |
| `removeEmptyVars`                  | Bool   | Удалять ли шаблоны переменных, которые оказались пустыми из нового имени ("#{var}" -> ""). По умолчанию - false.                  |
| `replace`                          | Array  | Массив из значений, которые нужно заменить.                                                                                       |
| `replace[0]`                       | String | Строка, которую нужно заменить (поддерживаются регулярные выражения).                                                             |
| `replace[1]`                       | String | На что нужно заменить (поддерживается обращение к группам через $, $1, $2, ...).                                                  |
| `replace[2]`                       | String | В каком элементе выполнять замену (Не обязательный. Если не указан, замена будет выполнена во всех элементах).                    |
| `NodeProperties`                   | Object | Описывает шаблоны для нужных типов элементов.                                                                                     |
| `NodeProperties[currentNodeType]`  | Object | Наименование класса элемента (отображается в логах Jmeter при нажатии на кнопку).                                                 |
| `skipDisabled`                     | Bool   | Пропускать ли выключенные элементы. По умолчанию - false.                                                                         |
| `disableJmeterVars`                | Bool   | Деактивировать ли переменные Jmeter ("${}" -> "{}"). По умолчанию - true.                                                         |
| `debugPrintConditionsResult`       | Bool   | Отображать результаты условий. По умолчанию - false.                                                                              |
| `search`                           | Array  | Массив поисков.                                                                                                                   |
| `search[*].searchIn`               | String | Строка, в которой будет выполняться поиск. Можно использовать переменные.                                                         |
| `search[*].searchReg`              | String | Что нужно искать. Можно использовать регулярные выражения. Обязательно наличие группы.                                            |
| `search[*].searchRegGroup`         | Int    | Группа, которая будет записана в переменную.                                                                                      |
| `search[*].searchOutVar`           | String | Название переменной, куда будет записан результат. Можно использовать модификаторы области (global., parent., ...).               |
| `search[*].leftRightSymbols`       | Array  | Символы, которые будут добавлены в итоговое значение. По умолчанию - ["", ""].                                                    |
| `search[*].searchDefault`          | String | Значение, которое будет использовано, если ничего не будет найдено.                                                               |
| `conditions`                       | Array  | Массив условий. Пробегается до первого полностью выполненного условия для текущего элемента.                                      |
| `conditions[*].inParentType`       | Array  | Массив типов элементов. Проверяет, находится ли текущий элемент в одном из указанных элементов.                                   |
| `conditions[*].strEquals[0]`       | String | Проверяет, равна ли строка значению. Можно использовать переменные.                                                               |
| `conditions[*].strEquals[1]`       | String | Чему равна строка.                                                                                                                |
| `conditions[*].strContains[0]`     | String | Проверяет наличие подстроки в строке.                                                                                             |
| `conditions[*].strContains[1]`     | String | Подстрока.                                                                                                                        |
| `conditions[*].minLevel`           | Int    | Проверяет, больше ли либо равен текущий уровень элемента значению.                                                                |
| `conditions[*].maxLevel`           | Int    | Проверяет, меньше ли либо равен текущий уровень элемента значению.                                                                |
| `conditions[*].currentLevel`       | Int    | Проверяет, равен ли текущий уровень элемента значению.                                                                            |
| `conditions[*].skip`               | Bool   | Пропустить элемент, т.е. не выполнять никаких действий с ним.                                                                     |
| `conditions[*].counterCommands`    | String | Строка, в которой можно менять значения счетчиков через обычный вызов.                                                            |
| `conditions[*].putVar[0]`          | String | Создать либо изменить переменную.                                                                                                 |
| `conditions[*].putVar[1]`          | String | Значение, которое нужно сохранить. Можно использовать переменные.                                                                 |
| `conditions[*].template`           | String | Шаблон, который будет применен для элемента.                                                                                      |
| `template`                         | String | Шаблон для элемента по умолчанию, если все блоки условий false.                                                                   |
| `variables`                        | Object | Создание переменных, видимых на всех уровнях дерева. Обращение к ним происходит с префиксом global., например, #{global.varName}. |
| `variables[varName]`               | String | Значение переменной.                                                                                                              |
| `counters`                         | Object | Создание счетчиков со своими настройками.                                                                                         |
| `counters[counterName]`            | Object | Имя счетчика.                                                                                                                     |
| `counters[counterName].startValue` | Int    | Начало счетчика (по умолчанию 0).                                                                                                 |
| `counters[counterName].endValue`   | Int    | Конец счетчика (по умолчанию null (нет конца)).                                                                                   |
| `counters[counterName].increment`  | Int    | Прирост (по умолчанию 1).                                                                                                         |

### Доступные переменные для каждого типа элемента
Каждому элементу в дереве известны параметры свои и родителя, так же глобальные переменные и создаваемые </br>
Абсолютно для любого типа элемента доступны переменные #{name} и #{comment}

## ----- SAMPLERS -----

### HTTPSamplerProxy
`protocol`</br>
`host`</br>
`path`</br>
`method` - метод запроса</br>
`params` - полная строка параметров имеющихся у запроса</br>
`param.№` - порядковый номер параметра</br>

### DebugSampler
`jmeterProperties` - checkbox</br>
`jmeterVariables` - checkbox</br>
`systemProperties` - checkbox</br>

## ----- LOGIC CONTROLLERS -----
### TransactionController
`isGenerate` - checkbox</br>
`isInclude` - checkbox</br>

### ModuleController
`selectedElementName` - название выбраного элемента</br>

### IfController
`condition` - строка условия</br>
`interpretCondition` - checkbox</br>
`evaluateForAllChildren` - checkbox</br>

### LoopController
`loopString` - указанная строка в элементе. Если бесконечно, то вернет -1</br>

### WhileController
`condition` - строка условия</br>

### IncludeController
`fullFilename` - полная строка указанного пути</br>
`filename` - только название файла</br>

### RunTime
`runtime` - строка runtime</br>

### ThroughputController
`basedOn` - выбранный стиль</br>
`throughput` - указанный процент</br>
`perUser` - checkbox</br>

### SwitchController
`switchValue` - указанная строка</br>

## ----- POST PROCESSORS -----
### JSONPostProcessor
`varName` - указання переменная для результата</br>
`jsonPath`</br>
`matchNumber`</br>
`defaultValue`</br>

### JMESPathExtractor
`varName` - указання переменная для результата</br>
`jmesPath`</br>
`matchNumber`</br>
`defaultValue`</br>

### BoundaryExtractor
`varName` - указання переменная для результата</br>
`leftBoundary`</br>
`rightBoundary`</br>
`matchNumber`</br>
`defaultValue`</br>

### RegexExtractor
`varName` - указання переменная для результата</br>
`regex`</br>
`template`</br>
`matchNumber`</br>
`defaultValue`</br>

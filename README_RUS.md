Jmeter Names Templater

Jmeter Names Templater - это плагин для Jmeter, который позволяет задавать шаблоны наименования элементам и переименовывать их на основе описанного конфигурационного файла.

## Установка

1. Скопируйте файл плагина в папку `lib/ext` вашего Jmeter.
2. Перезапустите Jmeter.

## Использование
После установки jar файла. В Jmeter появится новая кнопка. При нажатии каждый раз заного считывается конфиг файла
![image](https://github.com/user-attachments/assets/e39e8f75-9f1e-4b71-ac4f-71999a50d421)

После нажатия на кнопки так же в логах Jmeter пишется дерево элементов, которое несет дополнительную информацию об уровне элемента и о типе (Если флаг debugEnable = true)
`Rename Tree Button` - пробег по дереву и переименовывание начнется с начала Test Plan
`Rename Selected Tree Button` - пробег по дереву начнется с выделенного элемента, переименовываться будет только элементы выделенного элемента
`Print Tree Button` - отписать дерево в логах, без каких либо действий
Пример:
```
2024-07-21 21:05:58,393 INFO c.e.j.p.RunThroughTree: 
00: "Test Plan" (TestPlan)
01: |    "Thread Group" (ThreadGroup)
02: |    |    "Transaction Controller" (TransactionController)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
02: |    |    "Transaction Controller" (TransactionController)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
03: |    |    |    "HTTP Request" (HTTPSamplerProxy)
--------------------------------------------------
```

## Файл конфигурации

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
                "searchIn": ["#{name}", "_(\\d{3})_"],
                "searchOut": ["global.testGlobalVar_2", "_$1", ""]
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
                    "skip": false,
                    "counterCommands": "",
                    "putVar": ["global.testGlobalVar_2", "test"],
                    "template": "cond1_#{global.testGlobalVar_2}_#{myCounter(,02)}"
                }
            ],
            "template": "#{global.testGlobalVar_2}_#{myCounter(,02)}_#{protocol}_#{param.3}"
        }
    },
    "variables": {
        "testGlobalVar_1": "GLOBAL_VAR_1",
        "testGlobalVar_2": true,
        "testGlobalVar_3": 999
    },
    "counters": {
        "myCounter": {
            "start": 1,
            "end": 3,
            "increment": 1,
            "resetIf": [
                {
                    "levelEquals": [3],
                    "nodeType": ["TransactionController", "HTTPSamplerProxy"]
                }
            ]
        },
        "myCounter2": {
            "start": 15,
            "end": 30,
            "increment": 5
        }
    }
}
```

### Описание полей конфигурационного файла
```
- debugEnable [Bool] - Отображать в логах полное дерево скрипта. По умолчанию - true
- reloadAllTree [Bool] - Перезагружать после переименования полностью дерево. По умолчанию - false. Используется для отката всего дерева после переименования
- removeEmptyVars [Bool] - Удалять ли шаблоны переменных, которые оказались пустыми из нового имени ("#{var}" -> ""). По умолчанию - false

- replace [Array Arrays] - Массив из значений, которые нужно заменить
    - replace[0] [String] - Строка, которую нужно заменить (поддерживаются регулярные выражения)
    - replace[1] [String] - На что нужно заменить (поддерживается обращение к группам через $, $1, $2, ...)
    - replace[2] [String] - В каком элементе выполнять замену (Не обязательный. Если не указан, замена будет выполнена во всех элементах)

- NodeProperties [Object] - Описывает шаблоны для нужных типов элементов
    - nodeType [Object] - Наименование класса элемента (отображается в логах Jmeter при нажатии на кнопку)
        - skipDisabled [Bool] - Пропускать ли выключенные элементы. По умолчанию - false
        - disableJmeterVars [Bool] - Деактивировать ли переменные Jmeter ("${}" -> "{}"). По умолчанию - true
        - debugPrintConditionsResult [Bool] - Отображать результаты условий. По умолчанию - false
        
        - search [Array Objects] - Массив поисков
            - searchIn [Array] - блок для указания поиска
                - searchIn[0] [String] - строка в которой будет выполняться поиск (можно использовать переменные)
                - searchIn[1] [String] - регулярное выражения для поиска
            - searchOut [Array] - блок для итогов поиска
                - searchOut[0] [String] - переменная в которую будет записан результат поиска
                - searchOut[1] [String] - шаблон дял результата поиска. Можно ссылаться на группы через `$N`
                - searchOut[2] [String] - значение по умолчанию, если не было найдено результатов

        - conditions [Array Objects] - Массив условий. Пробегается до первого полностью выполненного условия для текущего элемента
            - inParentType [Array] - Массив типов элементов. Проверяет, находится ли текущий элемент в одном из указанных элементов
            - strEquals [Array] - проверка равняется ли строка значению
                - strEquals[0] [String] - Проверяет, равна ли строка значению. Можно использовать переменные
                - strEquals[1] [String] - Чему равна строка
            - strContains[Array] - проверка содержит ли строка подстроку
                - strContains[0] [String] - Проверяет наличие подстроки в строке
                - strContains[1] [String] - Подстрока
            - minLevel [Int] - Проверяет, больше ли либо равен текущий уровень элемента значению
            - maxLevel [Int] - Проверяет, меньше ли либо равен текущий уровень элемента значению
            - currentLevel [Int] - Проверяет, равен ли текущий уровень элемента значению
            - skip [Bool] - Пропустить элемент, т.е. не выполнять никаких действий с ним
            - counterCommands [String] - Строка, в которой можно менять значения счетчиков через обычный вызов
            - putVar [Array] - команда для сохранинение текста в переменную
                - putVar[0] [String] - Создать либо изменить переменную
                - putVar[1] [String] - Значение, которое нужно сохранить. Можно использовать переменные
            - template [String] - Шаблон, который будет применен для элемента
        
        - template [String] - Шаблон для элемента по умолчанию, если все блоки условий false
    
- variables [Object] - Создание переменных, видимых на всех уровнях дерева. Обращение к ним происходит с префиксом global., например, #{global.varName}
    - variables[varName] [String] - Значение переменной
    
- counters [Object] - Создание счетчиков со своими настройками
    - counterName [Object] - Имя счетчика
        - start [Int] - Начало счетчика (по умолчанию 0)
        - end [Int] - Конец счетчика (по умолчанию null (нет конца))
        - increment [Int] - Прирост (по умолчанию 1)
        - resetIf [Array Objects] - описание условий для автоматического сброса счетчика. Если указаны и levelEquals и nodeType, то условия будут работать по принципу cond1 AND cond2
            - levelEquals [Array] - список уровней на которых будет сбрасываться счетчик
            - nodeType [Array] - список типов элементов, на которых будет сбрасываться счетчик
```


### Обращение к счетчикам
Пример обращения к счетчикам:</br>
`#{name(command,format)}` - ВАЖНО! Если не указывать последние 2 параметра, то обязательно нужно сохранить запятые т.е. `#{name(,)}`</br>
`name` - имя счетчика.</br>
`command` - команда отвечающее за возвращаемое значение счетчика. По умолчанию указывается getAndAdd</br>
    - get - получить текущее число счетчика</br>
    - resetAndGet - сбросить счетчик до начального значения и вернуть число</br>
    - getAndAdd - получить текущее число счетчика, и добавить implement</br>
    - addAndGet - добавить implement, и получить текущее число счетчика</br>
`format` - выравнивание по длине, например если число счетчика 5, а формат 03, то вернется 005</br>

### Блок условий
Поля в блоке условий деляться на 2 типа - условия и действия</br>
Условия - `inParentType`, `strEquals`, `strContains`, `minLevel`, `maxLevel`, `currentLevel`</br>
Дейсвия - `skip`, `counterCommands`, `putVar`, `template`, `counterCommands`</br>
Действия выполняются только тогда, когда все указанные условия true</br>
Условия и действия можно комбинировать как угодно, но важно отметить, что условия работают по принципу condition AND condition</br>
В логах, при флаге `debugPrintConditionsResult = true`, можно увидить что типы условий, которые не указаны в блоке равны true, это необходимо для работы</br>
Описание действий:</br>
    - `skip` - если он true, то никаких действий с элементом не производится. Так же остальные действия не выполняются</br>
    - `putVar` - создание либо изменение переменной. Можно использовать модификаторы `global.`, `parent.`, ....</br>
    - `template` - шаблон который будет применен к элементу</br>
    - `counterCommands` - строка в которой можно вызвать счетчик (Работа со счетчиками описана выше)</br>
Если в блоке conditions несколько условий, то проверка условий будет выполняться до первого успешного</br>

### Модификаторы областей видимости
Модификаторы областей видимости нунжы для хранения и обращения к различным группам переменных</br>
Переменные без модификатора и `parent.` живут в пределах одного элемента, т.е. когда элемент обработается, в следующем элементе старые элементы уже не будут доступны (если у этих элементов один родитель, то ничего не изменится в области parent.)</br>
Переменные `global.` живут на протяжениии всего пробега по дереву, и могут быть вызваны и изменены в любой части дерева, так же как и счетчики</br>

### Доступные переменные для типов элементов
Каждому элементу в дереве известны параметры свои и родителя, так же глобальные переменные и создаваемые </br>
Что бы обратиться к параметрам родителям, нужно использовать модификатор `parent.`, например `parent.name`</br>
Абсолютно для любого типа элемента доступны переменные #{name} и #{comment}

## ----- SAMPLERS -----
### HTTPSamplerProxy
`#{protocol}`</br>
`#{host}`</br>
`#{path}`</br>
`#{method}` - метод запроса</br>
`#{params}` - полная строка параметров имеющихся у запроса</br>
`#{param.№}` - порядковый номер параметра</br>

### DebugSampler
`#{jmeterProperties}` - checkbox</br>
`#{jmeterVariables}` - checkbox</br>
`#{systemProperties}` - checkbox</br>

## ----- LOGIC CONTROLLERS -----
### TransactionController
`#{isGenerate}` - checkbox</br>
`#{isInclude}` - checkbox</br>

### ModuleController
`#{selectedElementName}` - название выбраного элемента</br>

### IfController
`#{condition}` - строка условия</br>
`#{interpretCondition}` - checkbox</br>
`#{evaluateForAllChildren}` - checkbox</br>

### LoopController
`#{loopString}` - указанная строка в элементе. Если бесконечно, то вернет -1</br>

### WhileController
`#{condition}` - строка условия</br>

### IncludeController
`#{fullFilename}` - полная строка указанного пути</br>
`#{filename}` - только название файла</br>

### RunTime
`#{runtime}` - строка runtime</br>

### ThroughputController
`#{basedOn}` - выбранный стиль</br>
`#{throughput}` - указанный процент</br>
`#{perUser}` - checkbox</br>

### SwitchController
`#{switchValue}` - указанная строка</br>

## ----- POST PROCESSORS -----
### JSONPostProcessor
`#{varName}` - указання переменная для результата</br>
`#{jsonPath}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### JMESPathExtractor
`#{varName}` - указання переменная для результата</br>
`#{jmesPath}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### BoundaryExtractor
`#{varName}` - указання переменная для результата</br>
`#{leftBoundary}`</br>
`#{rightBoundary}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### RegexExtractor
`#{varName}` - указання переменная для результата</br>
`#{regex}`</br>
`#{template}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

## Поддержка проекта
Если Вы хотите поддержать дальнейшее развитие плагина, вы можете сделать пожертвование 🙃 :
- 💳 **Сбербанк**: 2202 2017 2242 4294
- 🅿️ **[PayPal](https://www.paypal.me/DaniilGolikov)**</br>
Спасибо за вашу поддержку!

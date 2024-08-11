Jmeter Names Templater

Jmeter Names Templater is a plugin for Jmeter that allows you to set naming templates for elements and rename them based on a described configuration file.

## Installation

1. Copy the plugin file to the `lib/ext` folder of your Jmeter installation.
2. Restart Jmeter.

## Usage
After installing the jar file, a new button will appear in Jmeter. Each time you press it, the configuration file is re-read
![image](https://github.com/user-attachments/assets/a5e3d7ed-a1d9-40ee-affe-6ad2dd78d399)

After pressing the buttons, the Jmeter logs will also display the element tree, providing additional information about the element's level and type (if the debugEnable flag is set to true)
`Rename Tree Button` - traverses the tree and starts renaming from the beginning of the Test Plan
`Rename Selected Tree Button` - starts traversing the tree from the selected element, and only the elements under the selected item will be renamed
`Print Tree Button` - logs the tree without performing any actions
Example:
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

## Configuration File
The `renameConfig.json` file is used to describe templates for elements. The configuration file is located in `jmeter/bin/rename-config.json`</br>
Example of the file structure:
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

### Configuration File Fields Description
```
- debugEnable [Bool] - Displays the full script tree in the logs. Default - true
- reloadAllTree [Bool] - Reloads the entire tree after renaming. Default - false. Used to revert the entire tree after renaming
- removeEmptyVars [Bool] - Removes empty variable templates from the new name ("#{var}" -> ""). Default - false

- replace [Array Arrays] - Array of values that need to be replaced
    - replace[0] [String] - The string to be replaced (supports regular expressions)
    - replace[1] [String] - The replacement string (supports group references using $, $1, $2, ...)
    - replace[2] [String] - The element in which the replacement should be performed (Optional. If not specified, the replacement will be done in all elements)

- NodeProperties [Object] - Describes templates for specific types of elements
    - nodeType [Object] - The name of the element's class (displayed in the Jmeter logs when the buttons is pressed)
        - skipDisabled [Bool] - Whether to skip disabled elements. Default - false
        - disableJmeterVars [Bool] - Whether to deactivate Jmeter variables ("${}" -> "{}"). Default - true
        - debugPrintConditionsResult [Bool] - Whether to display the results of conditions. Default  - false
        
        - search [Array Objects] - Array of searches
            - searchIn [Array] - Block for specifying search criteria
                - searchIn[0] [String] - The string in which the search will be performed (variables can be used)
                - searchIn[1] [String] - The regular expression for the search
            - searchOut [Array] - Block for search results
                - searchOut[0] [String] - The variable to which the search result will be assigned
                - searchOut[1] [String] - Template for the search result. Group references can be made using `$N`
                - searchOut[2] [String] - Default value if no results were found

        - conditions [Array Objects] - Array of conditions. It iterates until the first fully met condition for the current element
            - inParentType [Array] - Array of element types. Checks whether the current element is within one of the specified elements
            - strEquals [Array] - Checks if the string equals the value
                - strEquals[0] [String] - Checks if the string equals the value (variables can be used)
                - strEquals[1] [String] - The value to which the string is compared
            - strContains[Array] - Checks if the string contains a substring
                - strContains[0] [String] - Checks for the presence of a substring in the string
                - strContains[1] [String] - The substring
            - minLevel [Int] - Checks if the current element's level is greater than or equal to the value
            - maxLevel [Int] - Checks if the current element's level is less than or equal to the value
            - currentLevel [Int] - Checks if the current element's level is equal to the value
            - skip [Bool] - Skips the element, i.e., no actions will be performed on it
            - counterCommands [String] -  string in which counter values can be changed through a simple command
            - putVar [Array] - Command to save text in a variable
                - putVar[0] [String] - Create or modify a variable
                - putVar[1] [String] - The value to be saved (variables can be used)
            - template [String] - The template that will be applied to the element
        
        - template [String] - Default template for the element if all condition blocks are false
    
- variables [Object] - Creates variables visible at all levels of the tree. Access them with the prefix  global., for example, #{global.varName}
    - variables[varName] [String] - The value of the variable
    
- counters [Object] - Creates counters with custom settings
    - counterName [Object] - The name of the counter
        - start [Int] - Counter start value (default  0)
        - end [Int] - Counter end value (default null (no end))
        - increment [Int] - Increment value (default  1)
        - resetIf [Array Objects] - Describes conditions for automatically resetting the counter. If both levelEquals and  nodeType, are specified, the conditions will work on a cond1 AND cond2 basis
            - levelEquals [Array] - List of levels where the counter will be reset
            - nodeType [Array] - List of element types where the counter will be reset
```

### Counter Access
ПExample of how to access counters:</br>
`#{name(command,format)}` - IMPORTANT! If you don't specify the last two parameters, you must still keep the commas, e.g. `#{name(,)}`</br>
`name` - The name of the counter</br>
`command` - The command that determines the counter's return value. By default, getAndAdd is used</br>
    - get - Get the current counter value</br>
    - resetAndGet - Reset the counter to its initial value and return the number</br>
    - getAndAdd - Get the current counter value and then add the increment</br>
    - addAndGet - Add the increment and then get the current counter value</br>
`format` - Length alignment, for example, if the counter value is 5 and the format is 03, the returned value will be 005</br>

### Conditions Block
Fields in the conditions block are divided into two types: conditions and actions</br>
Conditions - `inParentType`, `strEquals`, `strContains`, `minLevel`, `maxLevel`, `currentLevel`</br>
Actions - `skip`, `counterCommands`, `putVar`, `template`, `counterCommands`</br>
Actions are executed only when all specified conditions are true</br>
Conditions and actions can be combined in any way, but it's important to note that conditions work on an AND basis, meaning cond AND cond</br>
In the logs, with the `debugPrintConditionsResult = true`, мflag, you can see that the condition types not specified in the block are marked as true. This is necessary for the logic to work properly</br>
Actions Description:</br>
    - `skip` - If true, no actions are performed on the element. Other actions are also not executed</br>
    - `putVar` - Creates or modifies a variable. Modifiers like `global.`, `parent.`, ....</br>
    - `template` - The template that will be applied to the element</br>
    - `counterCommands` - A string where a counter can be invoked (working with counters is described above)</br>
If there are multiple conditions in the conditions block, the conditions are checked until the first successful one</br>

### Scope Modifiers
Scope modifiers are used to store and access different groups of variables</br>
Variables without a modifier and with `parent.` live within the scope of a single element. Once an element is processed, the old variables will no longer be available in the next element (if these elements share the same parent, nothing will change in the `parent.` scope)</br>
`global.` variables live throughout the entire tree traversal and can be called and modified in any part of the tree, just like counters</br>

### Available Variables for Element Types
Each element in the tree is aware of its own parameters and those of its parent, as well as global and created variables</br>
To access a parent's parameters, use the `parent.` modifier, for example, `parent.name.`</br>
For any element type, the variables #{name} and #{comment} are always available

## ----- SAMPLERS -----
### HTTPSamplerProxy
`#{protocol}`</br>
`#{host}`</br>
`#{path}`</br>
`#{method}` - HTTP method</br>
`#{params}` - Full string of request parameters</br>
`#{param.№}` - Parameter by its ordinal number</br>

### DebugSampler
`#{jmeterProperties}` - checkbox</br>
`#{jmeterVariables}` - checkbox</br>
`#{systemProperties}` - checkbox</br>

## ----- LOGIC CONTROLLERS -----
### TransactionController
`#{isGenerate}` - checkbox</br>
`#{isInclude}` - checkbox</br>

### ModuleController
`#{selectedElementName}` - Name of the selected element</br>

### IfController
`#{condition}` - Condition string</br>
`#{interpretCondition}` - checkbox</br>
`#{evaluateForAllChildren}` - checkbox</br>

### LoopController
`#{loopString}` - String specified in the element. Returns -1 if infinite</br>

### WhileController
`#{condition}` - Condition string</br>

### IncludeController
`#{fullFilename}` - Full string of the specified path</br>
`#{filename}` - Filename only</br>

### RunTime
`#{runtime}` - Runtime string</br>

### ThroughputController
`#{basedOn}` - Selected style</br>
`#{throughput}` - Specified percentage</br>
`#{perUser}` - checkbox</br>

### SwitchController
`#{switchValue}` - Specified string</br>

## ----- POST PROCESSORS -----
### JSONPostProcessor
`#{varName}` - Specified variable for the result</br>
`#{jsonPath}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### JMESPathExtractor
`#{varName}` - Specified variable for the result</br>
`#{jmesPath}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### BoundaryExtractor
`#{varName}` - Specified variable for the result</br>
`#{leftBoundary}`</br>
`#{rightBoundary}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

### RegexExtractor
`#{varName}` - Specified variable for the result</br>
`#{regex}`</br>
`#{template}`</br>
`#{matchNumber}`</br>
`#{defaultValue}`</br>

## Project Support
If you'd like to support the continued development of the plugin, you can make a donation 🙃:
- 💳 **Сбербанк**: 2202 2017 2242 4294
- 🅿️ **[PayPal](https://www.paypal.me/DaniilGolikov)**</br>
Thank you for your support!

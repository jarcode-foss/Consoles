
-- This program is used to list all unqiue types and functions provided by consoles. You can find documentation for official Lua libraries and functions online.
--
-- Written by Jarcode

function main()
    -- remove spaces
    local argument = args():gsub("%s+", "")
    -- check if empty args
    if (argument == "") then
        clear()
        printc("&eComputer functions:")
        printValues(reflect())
        write("\n")
        printc("&eComputer types:")
        printValues(uniqueTypeNames())
        printc("\nUse &cman [function]&f for information about a function, or &cfunctions [type]&f for a list of functions from a specific type.")
    else
        -- check for functions from a unique type
        local functionList = reduce(manual_functionNames(), function(entry) return startsWith(entry, argument) end);
        if (#functionList > 0) then
            clear()
            printc("Functions for type: &e" .. argument)
            printValues(functionList)
            printc("\nUse &cman [" .. argument .. ":function]&f for information on a function\n")
        else
            printc("Could not find any functions for type: &e" .. argument)
        end
    end
end

function reduce(tbl, predicate)
    local newTbl = {}
    for i = 1,#tbl do
       if (predicate(tbl[i])) then
           newTbl[#newTbl + 1] = tbl[i];
       end
    end
    return newTbl;
end

function printValues(tbl)
    for i = 1,#tbl do
        write(tbl[i])
        if i ~= #tbl then
            write("\t")
        else
            write("\n")
        end
    end
    return #tbl;
end

function startsWith(str, text)
    return string.sub(str, 1, string.len(text)) == text
end
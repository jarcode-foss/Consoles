--#desc A program that prints programs installed in the /bin folder, and points the user to this project's source code and the 'man' command.
--#author Jarcode
--#version 2.6
--#usage help
function main()
    local bin = resolveFolder("/bin")
    if bin == nil then
        write("help: /bin folder missing")
        return
    end
    print("Installed programs:")
    showEntries(providedList(bin))
    write("\t")
    showEntries(luaList(bin))
    write("\n\n")
    printc("Use &bman [program]&f for more information on a program")
    printc("Visit &9github.com/wacossusca34/Consoles/&f for source code & support.")
end

function showEntries(tbl)
    for i = 1,#tbl do
        writec(tbl[i])
        if i ~= #tbl then
            write("\t")
        end
    end
    return #tbl;
end

function providedList(folder)
    local fullList = folder:list()
    local files = folder:files()
    local provided = {}
    for i = 1,#fullList do
        if isFile(files, fullList[i]) == false then
            provided[#provided + 1] = "&c" .. fullList[i]
        end
    end
end

function luaList(folder)
    local files = folder:files()
    local list = {}
    for i = 1,#files do
        list[#list + 1] = "&e" .. files[i]:getName()
    end
end

function isFile(files, name)
    for i = 1,#files do
        if files[i]:getName() == name then return true end
    end
    return false
end
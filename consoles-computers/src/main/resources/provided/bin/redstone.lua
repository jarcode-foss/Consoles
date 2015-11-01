--#desc A simple program to toggle the first hooked up redstone input (if it exists).
--#author Jarcode
--#version 1.0
--#usage redstone (on/off)
blocks = redstoneLength()
if (blocks == 0) then
    print("There are no redstone blocks hooked up!")
else
    local arg = string.gsub(string.lower(args()), "%s$", "")
    if (arg == "on") then
        redstone(0, true)
    elseif (arg == "off") then
        redstone(0, false)
    else
        print("usage: redstone [on/off]")
    end
end
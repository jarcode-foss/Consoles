-- Written by Jarcode

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
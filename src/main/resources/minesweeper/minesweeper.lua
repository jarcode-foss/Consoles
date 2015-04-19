-- Written by Levi Webb

local blocks = require("block")

local GAME_WIDTH = 8;
local GAME_HEIGHT = 8;
local BLOCK_WIDTH = 32;
local BLOCK_HEIGHT = 32;
local H_MARGIN = 2;
local V_MARGIN = 2;

local blockFunction = function(x, y)
    return blocks[x][y]
end

for i = 1,GAME_WIDTH do
    blocks[i] = {}
    for k = 1,GAME_HEIGHT do
        blocks[i][k] = blocks.Block.new(i, k, blockFunction)
    end
end

local tasks = {}
local buffer
INST = {}

local function start()
    -- allocate screen buffer
    buffer = screenBuffer(4)
    -- switch screen session
    switchSession(4)
    local running = true
    local first = true
    while (running) do

        -- grab interaction events, store them into array
        local actions = {}
        local index = 1;
        local at = buffer:pollCoords()
        while (at ~= nil) do
            actions[index] = at;
            index = index + 1
            at = buffer:pollCoords()
        end

        -- iterate
        for i = 1,#actions do
            local x = (actions[i]:x() - H_MARGIN) / BLOCK_WIDTH
            local y = (actions[i]:y() - V_MARGIN) / BLOCK_HEIGHT
            local blk = blocks[x][y]
            if (blk ~= nil) then
                local result = blk:onClick()
                -- hit mine
                if (result) then
                    printc("&cYou lose!")
                    running = false
                end
            end
        end

        -- only update if someone interacted
        if (#actions > 0 or first) then
            -- create frame
            local frame = screenFrame();
            -- background
            frame:fill(68)
            -- render cycle
            for i = 1,GAME_WIDTH do
                for k = 1,GAME_HEIGHT do
                    local blk = blocks[i][k]
                    local ax = (BLOCK_WIDTH * i) + H_MARGIN;
                    local ay = (BLOCK_HEIGHT * i) + V_MARGIN;
                    local s = blk:getState()
                    if (s == blocks.state("COVERED")) then
                        iterate(ax, ay, function(x, y)
                            screen:set(x, y, 91)
                        end)
                        outline(ax, ay, function(x, y)
                            screen:set(x, y, 87)
                        end)
                    elseif (s == blocks.state("SHOWING")) then
                        iterate(ax, ay, function(x, y)
                            screen:set(x, y, 90)
                        end)
                        outline(ax, ay, function(x, y)
                            screen:set(x, y, 88)
                        end)
                        local count = blk:countAdj();
                        if (count > 0) then
                            local adj = "&1" .. count
                            local len = frame:len(adj)
                            local xoff = math.round((GAME_WIDTH / 2) - (len / 2))
                            local yoff = math.round((GAME_HEIGHT / 2) - 5)
                            screen:write(ax + xoff, ay + yoff, adj)
                        end
                    elseif (s == blocks.state("REVEALED")) then
                        iterate(ax, ay, function(x, y)
                            screen:set(x, y, 90)
                        end)
                        outline(ax, ay, function(x, y)
                            screen:set(x, y, 86)
                        end)
                    end
                end
            end
            -- end render cycle

            -- update buffer
            buffer:update(frame:id())
        end

        -- win condition
        local check = true
        for i = 1,GAME_WIDTH do
            for k = 1,GAME_HEIGHT do
                local blk = blocks[i][k]
                local s = blk:getState()
                if (s == blocks.state("COVERED") and not blk:isBomb()) then
                    check = false
                end
            end
        end

        if (check) then
            printc("&aYou win!")
            running = false
        end

        first = false

        -- wait a bit
        sleep(65)
    end
end

local function iterate(xoff, yoff, func)
    for x = xoff,BLOCK_WIDTH - 1 + xoff do
        for y = yoff,BLOCK_HEIGHT - 1 + yoff do
            func(x, y)
        end
    end
end

local function outline(xoff, yoff, func)
    for x = xoff,BLOCK_WIDTH - 1 + xoff do
        for y = yoff,BLOCK_HEIGHT - 1 + yoff do
            if (x == xoff or x == BLOCK_WIDTH - 1 + xoff
                    or y == yoff or y == BLOCK_HEIGHT - 1 + yoff) then
                func(x, y)
            end
        end
    end
end
INST.start = start;
INST.tasks = tasks;
INST.buffer = buffer;

--noinspection UnusedDef
function main(args)
    start();
end

return this;

print('Lua version: '.._VERSION)
while true do
	write('> ')
	local input = read()
	write(input..'\n')
	loadstring(input)()
end
